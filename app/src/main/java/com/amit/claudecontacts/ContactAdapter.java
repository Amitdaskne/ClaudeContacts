package com.amit.claudecontacts;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;
import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;
import java.util.List;
import java.util.Map;

public class ContactAdapter extends RecyclerView.Adapter<ContactAdapter.VH> {
    private List<Map<String, String>> list;
    private boolean isCloud;
    private OnDeleteClickListener deleteListener;

    public interface OnDeleteClickListener {
        void onDelete(String number);
    }

    public ContactAdapter(List<Map<String, String>> list, boolean isCloud, OnDeleteClickListener listener) {
        this.list = list;
        this.isCloud = isCloud;
        this.deleteListener = listener;
    }

    @NonNull @Override public VH onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        return new VH(LayoutInflater.from(parent.getContext()).inflate(R.layout.item_contact, parent, false));
    }

    @Override public void onBindViewHolder(@NonNull VH h, int pos) {
        Map<String, String> contact = list.get(pos);
        h.tvName.setText(contact.get("name"));
        h.tvNumber.setText(contact.get("number"));

        if (isCloud) {
            h.btnDelete.setVisibility(View.VISIBLE);
            h.btnDelete.setOnClickListener(v -> {
                if (deleteListener != null) deleteListener.onDelete(contact.get("number"));
            });
        } else {
            h.btnDelete.setVisibility(View.GONE);
        }
    }

    @Override public int getItemCount() { return list.size(); }

    public static class VH extends RecyclerView.ViewHolder {
        TextView tvName, tvNumber;
        ImageView btnDelete;
        public VH(@NonNull View itemView) {
            super(itemView);
            tvName = itemView.findViewById(R.id.tvName);
            tvNumber = itemView.findViewById(R.id.tvNumber);
            btnDelete = itemView.findViewById(R.id.btnDelete);
        }
    }
}
