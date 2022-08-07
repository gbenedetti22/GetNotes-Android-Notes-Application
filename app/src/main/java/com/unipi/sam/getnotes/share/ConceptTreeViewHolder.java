package com.unipi.sam.getnotes.share;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.TextView;

import com.unipi.sam.getnotes.R;
import com.unipi.sam.getnotes.table.Group;
import com.unnamed.b.atv.model.TreeNode;

public class ConceptTreeViewHolder extends TreeNode.BaseNodeViewHolder<ConceptTreeViewHolder.Item> {
    private TextView tvValue;
    private Item item;

    public ConceptTreeViewHolder(Context context) {
        super(context);
    }

    @Override
    public View createNodeView(TreeNode node, Item value) {
        LayoutInflater inflater = LayoutInflater.from(context);
        View view = inflater.inflate(R.layout.treeview_concept_layout, null, false);
        tvValue = view.findViewById(R.id.tree_concept_label);
        tvValue.setText(value.concept.getName());
        this.item = value;
        return view;
    }

    public TextView getTextView() {
        return tvValue;
    }

    public Group.Concept getConcept() {
        return item.concept;
    }

    public static class Item {
        private final Group.Concept concept;

        public Item(Group.Concept concept) {
            this.concept = concept;
        }
    }

}
