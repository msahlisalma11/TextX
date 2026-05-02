package devesh.app.ocr.adapter;

import android.content.Context;
import android.text.format.DateFormat;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageButton;
import android.widget.LinearLayout;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import java.util.Calendar;
import java.util.List;
import java.util.Locale;

import devesh.app.ocr.HistoryActivity;
import devesh.app.ocr.R;
import devesh.app.ocr.database.ScanFile;

public class HistoryAdapter extends RecyclerView.Adapter<HistoryAdapter.ViewHolder> {
    Context mContext;
    private List<ScanFile> localDataSet;

    public HistoryAdapter(Context context, List<ScanFile> dataSet) {
        localDataSet = dataSet;
        mContext = context;
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup viewGroup, int viewType) {
        View view = LayoutInflater.from(viewGroup.getContext())
                .inflate(R.layout.recycleview_history_item, viewGroup, false);
        return new ViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder viewHolder, int position) {
        ScanFile scan = localDataSet.get(position);

        String text = scan.text;
        if (text != null && text.length() > 20) {
            text = text.substring(0, 20).trim() + "...";
        }
        viewHolder.getTextView().setText(text);

        // Format date
        Calendar cal = Calendar.getInstance(Locale.ENGLISH);
        cal.setTimeInMillis(scan.time);
        String date = DateFormat.format("MMM dd, yyyy 'at' hh:mm a", cal).toString();
        viewHolder.getTvDate().setText(date);

        // Cycle through colors: Purple -> Green -> Yellow
        int colorIndex = position % 3;
        switch (colorIndex) {
            case 0:
                viewHolder.getCardContent().setBackgroundResource(R.drawable.history_card_purple);
                break;
            case 1:
                viewHolder.getCardContent().setBackgroundResource(R.drawable.history_card_green);
                break;
            case 2:
                viewHolder.getCardContent().setBackgroundResource(R.drawable.history_card_yellow);
                break;
        }

        viewHolder.getCardContent().setOnClickListener(view -> {
            if (mContext instanceof HistoryActivity) {
                ((HistoryActivity) mContext).OpenHistoryFile(viewHolder.getBindingAdapterPosition());
            }
        });

        if (viewHolder.getCopyButton() != null) {
            viewHolder.getCopyButton().setOnClickListener(view -> {
                if (mContext instanceof HistoryActivity) {
                    ((HistoryActivity) mContext).CopyText(viewHolder.getBindingAdapterPosition());
                }
            });
        }

        if (viewHolder.getShareButton() != null) {
            viewHolder.getShareButton().setOnClickListener(view -> {
                if (mContext instanceof HistoryActivity) {
                    ((HistoryActivity) mContext).ShareText(viewHolder.getBindingAdapterPosition());
                }
            });
        }
    }

    @Override
    public int getItemCount() {
        return localDataSet.size();
    }

    public void updateList(List<ScanFile> newList) {
        localDataSet = newList;
        notifyDataSetChanged();
    }

    public static class ViewHolder extends RecyclerView.ViewHolder {
        private final TextView textView;
        private final TextView tvDate;
        private final LinearLayout cardContent;
        private final ImageButton CopyButton;
        private final ImageButton ShareButton;

        public ViewHolder(View view) {
            super(view);
            textView = view.findViewById(R.id.textView);
            tvDate = view.findViewById(R.id.tvDate);
            cardContent = view.findViewById(R.id.cardContent);
            CopyButton = view.findViewById(R.id.CopyButton);
            ShareButton = view.findViewById(R.id.ShareButton);
        }

        public TextView getTextView() { return textView; }
        public TextView getTvDate() { return tvDate; }
        public LinearLayout getCardContent() { return cardContent; }
        public ImageButton getCopyButton() { return CopyButton; }
        public ImageButton getShareButton() { return ShareButton; }
    }
}
