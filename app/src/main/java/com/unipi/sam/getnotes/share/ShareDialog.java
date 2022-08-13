package com.unipi.sam.getnotes.share;

import android.app.AlertDialog;
import android.app.Dialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.res.Configuration;
import android.graphics.Color;
import android.graphics.Typeface;
import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.constraintlayout.widget.ConstraintLayout;
import androidx.core.content.res.ResourcesCompat;
import androidx.fragment.app.DialogFragment;
import androidx.fragment.app.FragmentManager;

import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.Task;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.GenericTypeIndicator;
import com.unipi.sam.getnotes.R;
import com.unipi.sam.getnotes.table.Group;
import com.unnamed.b.atv.model.TreeNode;
import com.unnamed.b.atv.view.AndroidTreeView;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Objects;

public class ShareDialog extends DialogFragment implements DialogInterface.OnClickListener, OnCompleteListener<DataSnapshot>, TreeNode.TreeNodeClickListener {
    private Group.Info info;
    private ConceptTreeViewHolder selectedHolder;
    private ConstraintLayout layout;
    private HashMap<String, ArrayList<Object>> storage;

    public interface ShareDialogListener {
        void onGroupChoosed(Group.Info info, Group.Concept g, ArrayList<Object> conceptFiles);
    }

    private ShareDialogListener listener;

    public ShareDialog(Group.Info info) {
        this.info = info;
    }

    @NonNull
    @Override
    public Dialog onCreateDialog(@Nullable Bundle savedInstanceState) {
        AlertDialog.Builder builder = new AlertDialog.Builder(getActivity());
        LayoutInflater inflater = requireActivity().getLayoutInflater();

        View view = inflater.inflate(R.layout.share_dialog, null);
        layout = view.findViewById(R.id.constraintLayoutTreeView);

        builder.setMessage("Scegli argomento..");
        builder.setView(view);
        builder.setPositiveButton("Condividi qui", this);
        builder.setNegativeButton("Annulla", this);
        FirebaseDatabase.getInstance().getReference()
                .child("groups")
                .child(info.getId())
                .child("storage")
                .get().addOnCompleteListener(this);
        return builder.create();
    }

    @Override
    public void onStart() {
        super.onStart();
        ((AlertDialog) Objects.requireNonNull(getDialog())).getButton(DialogInterface.BUTTON_POSITIVE).setEnabled(false);
    }

    @Override
    public void onAttach(@NonNull Context context) {
        super.onAttach(context);
        try {
            listener = (ShareDialogListener) context;
        } catch (ClassCastException e) {
            throw new ClassCastException(requireActivity()
                    + " must implement NoticeDialogListener");
        }
    }

    @Override
    public void onClick(DialogInterface dialog, int which) {
        switch (which) {
            case DialogInterface.BUTTON_POSITIVE: {
                if(listener != null) {
                    if (selectedHolder == null) {
                        listener.onGroupChoosed(info, null, storage.get("root"));
                        return;
                    }

                    listener.onGroupChoosed(info, selectedHolder.getConcept(), storage.get(selectedHolder.getConcept().getId()));
                }
                break;
            }
            case DialogInterface.BUTTON_NEGATIVE: {
                dismiss();
                break;
            }
        }
    }

    @Override
    public void onComplete(@NonNull Task<DataSnapshot> task) {
        if (task.isSuccessful()) {
            DataSnapshot snapshot = task.getResult();
            if (snapshot.exists()) {
                HashMap<String, ArrayList<Object>> storage = snapshot.getValue(new GenericTypeIndicator<HashMap<String, ArrayList<Object>>>() {
                });
                if(storage == null) return;

                this.storage = storage;
                buildTree(storage);
            }
        }

        ((AlertDialog) Objects.requireNonNull(getDialog())).getButton(DialogInterface.BUTTON_POSITIVE).setEnabled(true);
    }

    private void buildTree(HashMap<String, ArrayList<Object>> storage) {
        ArrayList<TreeNode> nodes = new ArrayList<>();

        ArrayList<Object> objs = storage.get("root");
        assert objs != null;
        for (Object o : objs) {
            Group.Concept c = Group.Concept.fromMap((HashMap<String, Object>) o);
            if(c == null) continue;

            TreeNode node = createNode(c);
            buildTreeRecursive(c, node, storage);
            nodes.add(node);
        }

        TreeNode root = TreeNode.root();
        root.addChildren(nodes);
        createTreeView(root);
    }


    private void buildTreeRecursive(Group.Concept concept, TreeNode rootNode, HashMap<String, ArrayList<Object>> storage) {
        if (!storage.containsKey(concept.getId())) {
            return;
        }

        ArrayList<Object> objs = storage.get(concept.getId());
        for (Object o : objs) {
            Group.Concept c = Group.Concept.fromMap((HashMap<String, Object>) o);
            if(c == null) continue;

            TreeNode node = createNode(c);
            rootNode.addChild(node);
            buildTreeRecursive(c, node, storage);
        }

    }

    private void createTreeView(TreeNode root) {
        AndroidTreeView tView = new AndroidTreeView(getActivity(), root);
        tView.setDefaultViewHolder(ConceptTreeViewHolder.class);
        tView.setDefaultContainerStyle(R.style.TreeNodeStyle);
        tView.setUse2dScroll(true);
        tView.setDefaultNodeClickListener(this);

        layout.addView(tView.getView());
    }

    private TreeNode createNode(Group.Concept c) {
        return new TreeNode(new ConceptTreeViewHolder.Item(c));
    }

    @Override
    public void onClick(TreeNode node, Object value) {
        boolean dark_mode = (getResources().getConfiguration().uiMode & Configuration.UI_MODE_NIGHT_MASK) == Configuration.UI_MODE_NIGHT_YES;
        if (selectedHolder != null) {
            selectedHolder.getTextView().setTextColor(dark_mode ? Color.WHITE : Color.BLACK);
            selectedHolder.getTextView().setTypeface(null, Typeface.NORMAL);
        }

        ConceptTreeViewHolder holder = (ConceptTreeViewHolder) node.getViewHolder();
        holder.getTextView().setTextColor(ResourcesCompat.getColor(getResources(), R.color.acquamarine, null));
        holder.getTextView().setTypeface(null, Typeface.BOLD);
        selectedHolder = holder;
    }
}
