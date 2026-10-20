package com.example.myapplication;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.FrameLayout;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.graphics.shapes.Utils;
import androidx.recyclerview.widget.RecyclerView;

import java.util.List;

public class FeatureAdapter extends RecyclerView.Adapter<FeatureAdapter.Holder> {

  private final List<Feature> list;
  private final OnFeatureClickListener listener;

  public FeatureAdapter(List<Feature> list, OnFeatureClickListener listener) {
    this.list = list;
    this.listener = listener;
  }

  @NonNull
  @Override
  public Holder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {

    View view =
        LayoutInflater.from(parent.getContext()).inflate(R.layout.feature_item, parent, false);

    return new Holder(view);
  }

  @Override
  public void onBindViewHolder(@NonNull Holder holder, int position) {

    Feature feature = list.get(position);

    holder.title.setText(feature.getTitle());
    holder.icon.setImageResource(feature.getIcon());
    holder.background.setBackgroundResource(feature.getBackground());

    holder.itemView.setOnClickListener(
        v -> {
          if (listener != null) {
            listener.onFeatureClick(feature);
          }
        });
  }

  @Override
  public int getItemCount() {
    return list.size();
  }

  static class Holder extends RecyclerView.ViewHolder {

    TextView title;
    ImageView icon;
    FrameLayout background;

    Holder(View itemView) {
      super(itemView);

      title = itemView.findViewById(R.id.title);
      icon = itemView.findViewById(R.id.icon);
      background = itemView.findViewById(R.id.iconBackground);
    }
  }

  public interface OnFeatureClickListener {
    void onFeatureClick(Feature feature);
  }
}
