package com.amit.claudecontacts;

import android.Manifest;
import android.content.pm.PackageManager;
import android.database.Cursor;
import android.os.Bundle;
import android.provider.ContactsContract;
import android.widget.Button;
import android.widget.Toast;
import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;
import androidx.fragment.app.Fragment;
import androidx.viewpager2.adapter.FragmentStateAdapter;
import androidx.viewpager2.widget.ViewPager2;
import com.google.android.material.tabs.TabLayout;
import com.google.android.material.tabs.TabLayoutMediator;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.database.FirebaseDatabase;
import java.util.HashMap;
import java.util.Map;

public class ContactsActivity extends AppCompatActivity {
    @Override
    protected void onCreate(Bundle b) {
        super.onCreate(b);
        setContentView(R.layout.activity_contacts);

        TabLayout tabLayout = findViewById(R.id.tabLayout);
        ViewPager2 viewPager = findViewById(R.id.viewPager);
        Button btnSync = findViewById(R.id.btnSync);

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
            if (ContextCompat.checkSelfPermission(this, Manifest.permission.READ_CONTACTS) == PackageManager.PERMISSION_GRANTED) {
                syncContacts();
            } else {
                ActivityCompat.requestPermissions(this, new String[]{Manifest.permission.READ_CONTACTS}, 100);
            }
        });
    }

    private void syncContacts() {
        String uid = FirebaseAuth.getInstance().getCurrentUser().getUid();
        Map<String, Object> contactsMap = new HashMap<>();
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

        FirebaseDatabase.getInstance().getReference("contacts").child(uid).setValue(contactsMap)
            .addOnCompleteListener(task -> {
                if (task.isSuccessful()) {
                    Toast.makeText(this, "Contacts Synced Successfully", Toast.LENGTH_SHORT).show();
                    // Refresh Cloud Tab
                    ((CloudContactsFragment)((FragmentStateAdapter) ((ViewPager2) findViewById(R.id.viewPager)).getAdapter()).createFragment(1)).refreshData();
                } else {
                    Toast.makeText(this, "Sync Failed. Check Internet.", Toast.LENGTH_SHORT).show();
                }
            });
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if (requestCode == 100 && grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
            syncContacts();
        }
    }
}
