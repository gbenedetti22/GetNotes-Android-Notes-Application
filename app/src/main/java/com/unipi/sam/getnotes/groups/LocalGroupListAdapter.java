package com.unipi.sam.getnotes.groups;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.google.android.material.card.MaterialCardView;
import com.unipi.sam.getnotes.R;
import com.unipi.sam.getnotes.table.Group;

import java.util.ArrayList;

public class LocalGroupListAdapter extends RecyclerView.Adapter<LocalGroupListAdapter.ViewHolder> {
    private ArrayList<Group.Info> groups = new ArrayList<>();

    public interface OnGroupClickListener {
        void OnGroupClick(Group.Info g);
    }
    private OnGroupClickListener listener;

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.group_card_view, parent, false);
        return new ViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        Group.Info g = groups.get(position);
        holder.groupName.setText(g.getGroupName());
        holder.info.setText("Creato da: " + g.getAuthorName());
        holder.letter.setText(String.valueOf(g.getGroupName().charAt(0)).toUpperCase());
        holder.cardView.setOnClickListener(v -> {
            if(listener != null)
                listener.OnGroupClick(g);
        });
    }

    public void setOnGroupClickListener(OnGroupClickListener listener) {
        this.listener = listener;
    }

    @Override
    public int getItemCount() {
        return groups.size();
    }

    public void setGroups(ArrayList<Group.Info> groups) {
        this.groups = groups;
        notifyDataSetChanged();
    }

    public static class ViewHolder extends RecyclerView.ViewHolder {
        private final TextView groupName;
        private final TextView info;
        private final TextView letter;
        private final MaterialCardView cardView;

        public ViewHolder(@NonNull View itemView) {
            super(itemView);

            groupName = itemView.findViewById(R.id.groupName);
            info = itemView.findViewById(R.id.infoLabel);
            letter = itemView.findViewById(R.id.icon_letter);
            cardView = itemView.findViewById(R.id.parent);
        }
    }
}
