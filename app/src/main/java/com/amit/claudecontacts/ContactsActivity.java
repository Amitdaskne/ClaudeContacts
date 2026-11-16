package com.amit.claudecontacts;

import android.Manifest;
import android.content.ContentProviderOperation;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.database.Cursor;
import android.os.Bundle;
import android.provider.ContactsContract;
import android.view.View;
import android.view.inputmethod.InputMethodManager;
import android.widget.Button;
import android.widget.ProgressBar;
import android.widget.Toast;
import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;
import androidx.core.view.WindowCompat;
import androidx.fragment.app.Fragment;
import androidx.viewpager2.adapter.FragmentStateAdapter;
import androidx.viewpager2.widget.ViewPager2;
import com.google.android.material.tabs.TabLayout;
import com.google.android.material.tabs.TabLayoutMediator;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.database.*;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;

public class ContactsActivity extends AppCompatActivity {
    private ViewPager2 viewPager;
    private ProgressBar pbSync, pbImport;

    @Override
    protected void onCreate(Bundle b) {
        super.onCreate(b);
        WindowCompat.setDecorFitsSystemWindows(getWindow(), false);
        setContentView(R.layout.activity_contacts);

        TabLayout tabLayout = findViewById(R.id.tabLayout);
        viewPager = findViewById(R.id.viewPager);
        Button btnSync = findViewById(R.id.btnSync);
        Button btnImport = findViewById(R.id.btnImport);
        Button btnLogout = findViewById(R.id.btnLogout);
        pbSync = findViewById(R.id.pbSync);
        pbImport = findViewById(R.id.pbImport);

        viewPager.setAdapter(new FragmentStateAdapter(this) {
            @NonNull @Override
            public Fragment createFragment(int position) {
                return position == 0 ? new DeviceContactsFragment() : new CloudContactsFragment();
            }
            @Override public int getItemCount() { return 2; }
        });

        new TabLayoutMediator(tabLayout, viewPager, (tab, position) -> {
            tab.setText(position == 0 ? "Device Contacts" : "Cloud Contacts");
        }).attach();

        btnSync.setOnClickListener(v -> {
            animateButton(v);
            hideKeyboard();
            checkPermissionAndSync(btnSync, pbSync);
        });
        
        btnImport.setOnClickListener(v -> {
            animateButton(v);
            hideKeyboard();
            importContactsFromCloud(btnImport, pbImport);
        });
        
        btnLogout.setOnClickListener(v -> {
            animateButton(v);
            FirebaseAuth.getInstance().signOut();
            startActivity(new Intent(this, LoginActivity.class));
            finish();
        });
    }

    private void animateButton(View v) {
        v.animate().scaleX(0.9f).scaleY(0.9f).setDuration(100).withEndAction(() -> 
            v.animate().scaleX(1f).scaleY(1f).setDuration(100).start()
        ).start();
    }

    private void hideKeyboard() {
        InputMethodManager imm = (InputMethodManager) getSystemService(Context.INPUT_METHOD_SERVICE);
        if (imm != null && getCurrentFocus() != null) {
            imm.hideSoftInputFromWindow(getCurrentFocus().getWindowToken(), 0);
        }
    }

    private void checkPermissionAndSync(Button btn, ProgressBar pb) {
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.READ_CONTACTS) == PackageManager.PERMISSION_GRANTED) {
            syncContacts(btn, pb);
        } else {
            ActivityCompat.requestPermissions(this, new String[]{Manifest.permission.READ_CONTACTS}, 100);
        }
    }

    private void syncContacts(Button btn, ProgressBar pb) {
        btn.setText("");
        pb.setVisibility(View.VISIBLE);
        btn.setEnabled(false);

        String uid = FirebaseAuth.getInstance().getCurrentUser().getUid();
        Map<String, Object> contactsMap = new HashMap<>();
        
        new Thread(() -> {
            Cursor cursor = getContentResolver().query(ContactsContract.CommonDataKinds.Phone.CONTENT_URI, null, null, null, null);
            if (cursor != null) {
                while (cursor.moveToNext()) {
                    String name = cursor.getString(cursor.getColumnIndexOrThrow(ContactsContract.CommonDataKinds.Phone.DISPLAY_NAME));
                    String number = cursor.getString(cursor.getColumnIndexOrThrow(ContactsContract.CommonDataKinds.Phone.NUMBER));
                    number = number.replaceAll("[^0-9+]", "");
                    Map<String, String> contactInfo = new HashMap<>();
                    contactInfo.put("name", name);
                    contactInfo.put("number", number);
                    contactsMap.put(number, contactInfo);
                }
                cursor.close();
            }

            runOnUiThread(() -> {
                FirebaseDatabase.getInstance().getReference("contacts").child(uid).setValue(contactsMap)
                    .addOnCompleteListener(task -> {
                        pb.setVisibility(View.GONE);
                        btn.setEnabled(true);
                        btn.setText("Sync to Cloud");
                        if (task.isSuccessful()) {
                            Toast.makeText(this, "Synced Successfully", Toast.LENGTH_SHORT).show();
                            viewPager.setCurrentItem(1);
                        } else {
                            Toast.makeText(this, "Sync Failed", Toast.LENGTH_SHORT).show();
                        }
                    });
            });
        }).start();
    }

    private void importContactsFromCloud(Button btn, ProgressBar pb) {
        btn.setText("");
        pb.setVisibility(View.VISIBLE);
        btn.setEnabled(false);
        
        String uid = FirebaseAuth.getInstance().getCurrentUser().getUid();
        FirebaseDatabase.getInstance().getReference("contacts").child(uid).addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                if (!snapshot.exists()) {
                    Toast.makeText(ContactsActivity.this, "No contacts in Cloud", Toast.LENGTH_SHORT).show();
                    resetImportBtn(btn, pb);
                    return;
                }

                new Thread(() -> {
                    ArrayList<ContentProviderOperation> ops = new ArrayList<>();
                    for (DataSnapshot ds : snapshot.getChildren()) {
                        String name = ds.child("name").getValue(String.class);
                        String number = ds.child("number").getValue(String.class);
                        if (name != null && number != null) {
                            ops.add(ContentProviderOperation.newInsert(ContactsContract.RawContacts.CONTENT_URI)
                                    .withValue(ContactsContract.RawContacts.ACCOUNT_TYPE, null)
                                    .withValue(ContactsContract.RawContacts.ACCOUNT_NAME, null).build());
                            ops.add(ContentProviderOperation.newInsert(ContactsContract.Data.CONTENT_URI)
                                    .withValueBackReference(ContactsContract.Data.RAW_CONTACT_ID, ops.size() - 1)
                                    .withValue(ContactsContract.Data.MIMETYPE, ContactsContract.CommonDataKinds.StructuredName.CONTENT_ITEM_TYPE)
                                    .withValue(ContactsContract.CommonDataKinds.StructuredName.DISPLAY_NAME, name).build());
                            ops.add(ContentProviderOperation.newInsert(ContactsContract.Data.CONTENT_URI)
                                    .withValueBackReference(ContactsContract.Data.RAW_CONTACT_ID, ops.size() - 2)
                                    .withValue(ContactsContract.Data.MIMETYPE, ContactsContract.CommonDataKinds.Phone.CONTENT_ITEM_TYPE)
                                    .withValue(ContactsContract.CommonDataKinds.Phone.NUMBER, number)
                                    .withValue(ContactsContract.CommonDataKinds.Phone.TYPE, ContactsContract.CommonDataKinds.Phone.TYPE_MOBILE).build());
                        }
                    }
                    try {
                        getContentResolver().applyBatch(ContactsContract.AUTHORITY, ops);
                        runOnUiThread(() -> {
                            Toast.makeText(ContactsActivity.this, "Imported to Device!", Toast.LENGTH_SHORT).show();
                            viewPager.setCurrentItem(0);
                            resetImportBtn(btn, pb);
                        });
                    } catch (Exception e) {
                        runOnUiThread(() -> {
                            Toast.makeText(ContactsActivity.this, "Import Failed", Toast.LENGTH_SHORT).show();
                            resetImportBtn(btn, pb);
                        });
                    }
                }).start();
            }
            @Override public void onCancelled(@NonNull DatabaseError error) { resetImportBtn(btn, pb); }
        });
    }

    private void resetImportBtn(Button btn, ProgressBar pb) {
        pb.setVisibility(View.GONE);
        btn.setEnabled(true);
        btn.setText("Import to Device");
    }
}
