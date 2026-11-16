package com.amit.claudecontacts;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;
import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;
import java.util.List;
import java.util.Map;

public class ContactAdapter extends RecyclerView.Adapter<ContactAdapter.VH> {
    private List<Map<String, String>> list;
    public ContactAdapter(List<Map<String, String>> list) { this.list = list; }

    @NonNull @Override public VH onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        return new VH(LayoutInflater.from(parent.getContext()).inflate(R.layout.item_contact, parent, false));
    }
    @Override public void onBindViewHolder(@NonNull VH h, int pos) {
        h.tvName.setText(list.get(pos).get("name"));
        h.tvNumber.setText(list.get(pos).get("number"));
    }
    @Override public int getItemCount() { return list.size(); }

    public static class VH extends RecyclerView.ViewHolder {
        TextView tvName, tvNumber;
        public VH(@NonNull View itemView) {
            super(itemView);
            tvName = itemView.findViewById(R.id.tvName);
            tvNumber = itemView.findViewById(R.id.tvNumber);
        }
    }
}
