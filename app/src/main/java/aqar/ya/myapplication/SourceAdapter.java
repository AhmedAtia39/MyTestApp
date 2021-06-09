package aqar.ya.myapplication;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Filter;
import android.widget.Filterable;
import android.widget.TextView;

import androidx.recyclerview.widget.RecyclerView;

import java.util.ArrayList;

import aqar.ya.myapplication.models.SourceModel;


public class SourceAdapter extends RecyclerView.Adapter<SourceAdapter.ViewHolder> implements Filterable {
    ArrayList<SourceModel> items;
    public ArrayList<SourceModel> filteredItems;
    Context context;
    View rootView;
    ISource iSource;

    public SourceAdapter(Context context, ArrayList<SourceModel> items, ISource iSource) {
        this.items = items;
        this.filteredItems = items;
        this.context = context;
        this.iSource = iSource;

    }

    @Override
    public ViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {

        rootView = LayoutInflater.from(context).inflate(R.layout.item_txt, parent, false);
        ViewHolder v = new ViewHolder(rootView);
        return v;
    }

    @Override
    public void onBindViewHolder(ViewHolder holder, final int position) {

        if (filteredItems != null) {
            holder.txtName.setText(filteredItems.get(position).getName());
        }


    }

    @Override
    public Filter getFilter() {
        return exampleFilter;
    }

    @Override
    public int getItemCount() {
        return filteredItems.size();
    }

    private final Filter exampleFilter = new Filter() {
        @Override
        protected FilterResults performFiltering(CharSequence constraint) {
            filteredItems = new ArrayList<>();
            if (constraint == null || constraint.length() == 0) {
                filteredItems.addAll(items);
            } else {
                String filterPattern = constraint.toString().toLowerCase().trim();
                for (SourceModel item : items) {
                    if (item.getName().toLowerCase().contains(filterPattern)) {
                        filteredItems.add(item);
                    }
                }
            }
            FilterResults results = new FilterResults();
            results.values = filteredItems;
            return results;
        }

        @Override
        protected void publishResults(CharSequence constraint, FilterResults results) {
            notifyDataSetChanged();
        }
    };

    @Override
    public int getItemViewType(int position) {
        return position;
    }

    public class ViewHolder extends RecyclerView.ViewHolder {

        TextView  txtName;

        public ViewHolder(final View itemView) {
            super(itemView);

            txtName = itemView.findViewById(R.id.txtName);


            txtName.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    iSource.onClick(filteredItems.get(getLayoutPosition()));
                }
            });
        }
    }
}
