package com.unipi.sam.getnotes.home;

import android.content.Context;
import android.database.Cursor;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.unipi.sam.getnotes.R;

import java.util.ArrayList;

public class IconsViewAdapter extends RecyclerView.Adapter<IconsViewAdapter.ViewHolder> {
    private static final String TAG = "IconsViewAdapter";
    private ArrayList<HomeIcon> homeIcons = new ArrayList<>();
    private Cursor cursor;

    public interface IconTouchListener {
        void onIconCreated(ViewHolder icon);
    }
    private IconTouchListener listener;

    public IconsViewAdapter(Context context) {
        listener = (IconTouchListener) context;
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        ViewHolder holder;

        switch (viewType) {
            case HomeIcon.TYPE_FOLDER : {
                View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.folder_card_view, parent, false);
                holder = new ViewHolder(view);
                holder.setAsFolder();
                holder.type = HomeIcon.TYPE_FOLDER;
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

    public void setCursor(Cursor cursor) {
        if(cursor == this.cursor) return;
        if(this.cursor != null) {
            this.cursor.close();
        }

        this.cursor = cursor;
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        cursor.moveToPosition(position);
        holder.id = cursor.getInt(0);
        holder.parent_folder = cursor.getInt(1);
        String name = cursor.getString(2);

        holder.setText(name);
        listener.onIconCreated(holder);
    }

    @Override
    public int getItemCount() {
        return cursor == null ? 0 : cursor.getCount();
    }

    @Override
    public int getItemViewType(int position) {
        cursor.moveToPosition(position);
        String type = cursor.getString(4);

        switch (type) {
            case "NOTE" : return HomeIcon.TYPE_NOTE;
            case "FOLDER": return HomeIcon.TYPE_FOLDER;
            default: return -1;
        }
    }

    public static class ViewHolder extends RecyclerView.ViewHolder {
        private int id;
        private String name;
        private int type;
        private int parent_folder;
        private final View itemView;
        private TextView label;

        public ViewHolder(@NonNull View itemView) {
            super(itemView);
            this.itemView = itemView;
        }

        private void setAsFolder() {
            label = itemView.findViewById(R.id.folder_label);
        }

        private void setAsNote() {
            label = itemView.findViewById(R.id.note_label);
        }

        private void setText(String name) {
            label.setText(name);
            this.name = name;
        }

        public int getId() {
            return id;
        }

        public int getParentFolder() {
            return parent_folder;
        }

        public int getType() {
            return type;
        }

        public String getName() {
            return name;
        }

        public void setId(int id) {
            this.id = id;
        }

        public void setOnClickListener(View.OnClickListener listener) {
            itemView.setOnClickListener(listener);
        }
    }

}
