package com.amit.claudecontacts;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;
import androidx.annotation.NonNull;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.database.*;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

public class CloudContactsFragment extends Fragment {
    private ContactAdapter adapter;
    private List<Map<String, String>> list = new ArrayList<>();
    private TextView tvEmpty;
    private ProgressBar pb;

    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, ViewGroup container, Bundle b) {
        View v = inflater.inflate(R.layout.fragment_contacts, container, false);
        RecyclerView rv = v.findViewById(R.id.recyclerView);
        tvEmpty = v.findViewById(R.id.tvEmpty);
        pb = v.findViewById(R.id.progressBar);
        rv.setLayoutManager(new LinearLayoutManager(getContext()));
        
        adapter = new ContactAdapter(list, true, this::deleteContact);
        rv.setAdapter(adapter);
        
        refreshData();
        return v;
    }

    public void refreshData() {
        if (FirebaseAuth.getInstance().getCurrentUser() == null) return;
        pb.setVisibility(View.VISIBLE);
        tvEmpty.setVisibility(View.GONE);
        String uid = FirebaseAuth.getInstance().getCurrentUser().getUid();
        
        FirebaseDatabase.getInstance().getReference("contacts").child(uid).addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                list.clear();
                for (DataSnapshot ds : snapshot.getChildren()) {
                    Map<String, String> map = (Map<String, String>) ds.getValue();
                    list.add(map);
                }
                adapter.notifyDataSetChanged();
                pb.setVisibility(View.GONE);
                tvEmpty.setVisibility(list.isEmpty() ? View.VISIBLE : View.GONE);
            }
            @Override public void onCancelled(@NonNull DatabaseError error) { pb.setVisibility(View.GONE); }
        });
    }

    private void deleteContact(String number) {
        String uid = FirebaseAuth.getInstance().getCurrentUser().getUid();
        FirebaseDatabase.getInstance().getReference("contacts").child(uid).child(number).removeValue()
            .addOnSuccessListener(aVoid -> {
                Toast.makeText(getContext(), "Contact deleted from cloud", Toast.LENGTH_SHORT).show();
                refreshData();
            });
    }
}
