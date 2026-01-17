package com.example.glbmodelmanager.adapter;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageButton;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.example.glbmodelmanager.R;
import com.example.glbmodelmanager.data.GlbModel;
import com.google.android.material.button.MaterialButton;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Locale;

/**
 * Adapter for displaying GLB models in RecyclerView
 * Handles both Admin and User views
 */
public class GlbModelAdapter extends RecyclerView.Adapter<GlbModelAdapter.ModelViewHolder> {

    private List<GlbModel> models = new ArrayList<>();
    private boolean isAdmin;
    private OnItemClickListener listener;

    /**
     * Interface for handling click events
     */
    public interface OnItemClickListener {
        void onViewClick(GlbModel model);
        void onDeleteClick(GlbModel model);
    }

    /**
     * Constructor
     */
    public GlbModelAdapter(boolean isAdmin, OnItemClickListener listener) {
        this.isAdmin = isAdmin;
        this.listener = listener;
    }

    /**
     * ViewHolder class holds references to views in each item
     */
    public static class ModelViewHolder extends RecyclerView.ViewHolder {
        TextView tvModelName;
        TextView tvModelSize;
        TextView tvModelDate;
        MaterialButton btnView;
        ImageButton btnDelete;

        public ModelViewHolder(@NonNull View itemView) {
            super(itemView);
            tvModelName = itemView.findViewById(R.id.tvModelName);
            tvModelSize = itemView.findViewById(R.id.tvModelSize);
            tvModelDate = itemView.findViewById(R.id.tvModelDate);
            btnView = itemView.findViewById(R.id.btnView);
            btnDelete = itemView.findViewById(R.id.btnDelete);
        }
    }

    @NonNull
    @Override
    public ModelViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.item_glb_model, parent, false);
        return new ModelViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull ModelViewHolder holder, int position) {
        GlbModel model = models.get(position);

        // Set model name
        holder.tvModelName.setText(model.getName());

        // Format and set file size
        holder.tvModelSize.setText(formatFileSize(model.getFileSize()));

        // Format and set date
        SimpleDateFormat dateFormat = new SimpleDateFormat("MMM dd, yyyy", Locale.getDefault());
        holder.tvModelDate.setText("Added: " + dateFormat.format(new Date(model.getAddedDate())));

        // Handle View button click
        holder.btnView.setOnClickListener(v -> {
            if (listener != null) {
                listener.onViewClick(model);
            }
        });

        // Show/hide delete button based on user role
        if (isAdmin) {
            holder.btnDelete.setVisibility(View.VISIBLE);
            holder.btnDelete.setOnClickListener(v -> {
                if (listener != null) {
                    listener.onDeleteClick(model);
                }
            });
        } else {
            holder.btnDelete.setVisibility(View.GONE);
        }
    }

    @Override
    public int getItemCount() {
        return models.size();
    }

    /**
     * Update the list of models
     * Called when data changes in database
     */
    public void submitList(List<GlbModel> newModels) {
        this.models = newModels;
        notifyDataSetChanged();
    }

    /**
     * Helper function to format file size in human-readable format
     */
    private String formatFileSize(long size) {
        if (size < 1024) {
            return size + " B";
        } else if (size < 1024 * 1024) {
            return String.format(Locale.getDefault(), "%.2f KB", size / 1024.0);
        } else {
            return String.format(Locale.getDefault(), "%.2f MB", size / (1024.0 * 1024.0));
        }
    }
}

