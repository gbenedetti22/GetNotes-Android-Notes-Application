package com.unipi.sam.getnotes.groups;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.unipi.sam.getnotes.R;
import com.unipi.sam.getnotes.home.HomeIcon;
import com.unipi.sam.getnotes.table.Group;

import java.util.ArrayList;

public class GroupStorageAdapter extends RecyclerView.Adapter<GroupStorageAdapter.ViewHolder> {
    private ArrayList<Object> files = new ArrayList<>();
    private final int TYPE_CONCEPT = 2;
    private String currentPosition = "root";

    public interface OnIconClickListener {
        void onConceptIconClick(String clickedConceptID, String parentFolder);
        void onNoteIconClick(String id);
    }
    private OnIconClickListener listener;

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        ViewHolder holder;

        switch (viewType) {
            case TYPE_CONCEPT: {
                View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.concept_card_view, parent, false);
                holder = new ViewHolder(view);
                holder.setAsConcept();
                holder.type = TYPE_CONCEPT;
                break;
            }
            case HomeIcon.TYPE_NOTE: {
                View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.note_card_view, parent, false);
                holder = new ViewHolder(view);
                holder.setAsNote();
                holder.type = HomeIcon.TYPE_NOTE;
                break;
            }
            default:
                throw new IllegalStateException("Unexpected value: " + viewType);
        }

        return holder;
    }

    public void setOnIconClickListener(OnIconClickListener listener) {
        this.listener = listener;
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        Object file = files.get(position);

        switch (getItemViewType(position)) {
            case TYPE_CONCEPT: {
                Group.Concept concept = (Group.Concept) file;
                holder.id = concept.getId();
                holder.label.setText(concept.getName());
                holder.parent_folder = currentPosition;
                holder.itemView.setOnClickListener(v -> {
                    if(listener != null) {
                        listener.onConceptIconClick(concept.getId(), holder.parent_folder);
                        currentPosition = concept.getId();
                    }
                });
                break;
            }

            case HomeIcon.TYPE_NOTE: {
                Group.Note note = (Group.Note) file;
                holder.id = note.getId();
                holder.label.setText(note.getName());
                holder.parent_folder = currentPosition;
                holder.itemView.setOnClickListener(v -> {
                    if(listener != null)
                        listener.onNoteIconClick(note.getId());
                });
                break;
            }
        }
    }

    @Override
    public int getItemViewType(int position) {
        Object file = files.get(position);

        if (file instanceof Group.Concept) return TYPE_CONCEPT;
        if (file instanceof Group.Note) return HomeIcon.TYPE_NOTE;
        return -1;
    }

    @Override
    public int getItemCount() {
        return files.size();
    }

    public ArrayList<Object> getFiles() {
        return files;
    }

    public void clear() {
        files.clear();
        notifyDataSetChanged();
    }

    public void setFiles(ArrayList<Object> files) {
        this.files = files;
        notifyDataSetChanged();
    }

    public String getCurrentPosition() {
        return currentPosition;
    }

    public static class ViewHolder extends RecyclerView.ViewHolder {
        private String id;
        private String name;
        private int type;
        private String parent_folder;
        private final View itemView;
        private TextView label;

        public ViewHolder(@NonNull View itemView) {
            super(itemView);
            this.itemView = itemView;
        }

        private void setAsConcept() {
            label = itemView.findViewById(R.id.concept_label);
        }

        public void setAsNote() {
            label = itemView.findViewById(R.id.note_label);
        }

        public String getId() {
            return id;
        }
    }
}
