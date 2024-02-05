package bd.stock.njoystick.Models;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import java.util.List;
public class ProductAdapter extends RecyclerView.Adapter<ProductAdapter.ViewHolder> {

    private final List<Producto> productList;
    private final LayoutInflater inflater;
    private OnItemClickListener listener;

    public interface OnItemClickListener {
        void onItemClick(View view, int position);
    }

    public void setOnItemClickListener(OnItemClickListener listener) {
        this.listener = listener;
    }

    public ProductAdapter(Context context, List<Producto> productList) {
        this.inflater = LayoutInflater.from(context);
        this.productList = productList;
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = inflater.inflate(android.R.layout.simple_list_item_1, parent, false);
        return new ViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        Producto producto = productList.get(position);
        holder.productNameTextView.setText(producto.getNombre());
    }

    @Override
    public int getItemCount() {
        return productList.size();
    }

    public Producto getItem(int position) {
        return productList.get(position);
    }

    public void clear() {
        productList.clear();
        notifyDataSetChanged();
    }

    public void addAll(List<Producto> items) {
        productList.clear();
        productList.addAll(items);
        notifyDataSetChanged();
    }

    public class ViewHolder extends RecyclerView.ViewHolder {
        final TextView productNameTextView;

        ViewHolder(View itemView) {
            super(itemView);
            productNameTextView = itemView.findViewById(android.R.id.text1);

            itemView.setOnClickListener(view -> {
                if (listener != null) {
                    int position = getAdapterPosition();
                    if (position != RecyclerView.NO_POSITION) {
                        listener.onItemClick(view, position);
                    }
                }
            });
        }
    }
}

