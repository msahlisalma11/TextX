package devesh.app.ocr;

import android.content.ClipData;
import android.content.ClipboardManager;
import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.SearchView;
import androidx.recyclerview.widget.LinearLayoutManager;

import java.util.ArrayList;
import java.util.List;
import java.util.Calendar;

import devesh.app.ocr.adapter.HistoryAdapter;
import devesh.app.ocr.database.DatabaseTool;
import devesh.app.ocr.database.ScanFile;
import devesh.app.ocr.databinding.ActivityHistoryBinding;

public class HistoryActivity extends AppCompatActivity implements SearchView.OnQueryTextListener {
    String TAG = "HistoryAct";
    ActivityHistoryBinding binding;
    DatabaseTool databaseTool;
    List<ScanFile> scanFileList = new ArrayList<>();
    List<ScanFile> filteredList = new ArrayList<>();
    HistoryAdapter mAdapter;

    // Variables pour le filtrage
    private String currentQuery = "";
    private String currentDateFilter = "all"; // all, today, week
    private boolean filterByKeywords = false;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        binding = ActivityHistoryBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());
        databaseTool = new DatabaseTool(this);

        // Setup RecyclerView une seule fois
        mAdapter = new HistoryAdapter(this, filteredList);
        binding.HistoryRecycleView.setLayoutManager(new LinearLayoutManager(this));
        binding.HistoryRecycleView.setAdapter(mAdapter);

        // Setup SearchView
        binding.searchViewHistory.setOnQueryTextListener(this);
        binding.searchViewHistory.setQueryHint("Rechercher dans l'historique...");

        setupFilterChips();

        binding.HistoryButton.setOnClickListener(view -> {
            databaseTool.clearHistory();
            scanFileList.clear();
            filteredList.clear();
            updateUI();
            Toast.makeText(this, "History Deleted", Toast.LENGTH_SHORT).show();
            finish();
        });
    }

    @Override
    public void onBackPressed() {
        super.onBackPressed();
    }

    @Override
    protected void onStart() {
        super.onStart();
        refreshData();
    }

    void refreshData() {
        scanFileList = databaseTool.getAll();
        applyFilters();
    }

    void updateUI() {
        if (mAdapter != null) {
            mAdapter.updateList(new ArrayList<>(filteredList));
        }

        // Afficher le message vide si aucun résultat
        if (filteredList.isEmpty()) {
            binding.emptyStateText.setVisibility(View.VISIBLE);
            binding.HistoryRecycleView.setVisibility(View.GONE);
        } else {
            binding.emptyStateText.setVisibility(View.GONE);
            binding.HistoryRecycleView.setVisibility(View.VISIBLE);
        }
    }

    void setupFilterChips() {
        // Chip "Has Keywords"
        binding.chipKeywords.setOnCheckedChangeListener((chip, isChecked) -> {
            filterByKeywords = isChecked;
            applyFilters();
        });

        // Chip "Today"
        binding.chipToday.setOnClickListener(v -> {
            currentDateFilter = "today";
            updateChipStates("today");
            applyFilters();
        });

        // Chip "This Week"
        binding.chipWeek.setOnClickListener(v -> {
            currentDateFilter = "week";
            updateChipStates("week");
            applyFilters();
        });

        // Chip "All Time"
        binding.chipAll.setOnClickListener(v -> {
            currentDateFilter = "all";
            updateChipStates("all");
            applyFilters();
        });
    }

    void updateChipStates(String selected) {
        binding.chipToday.setChecked(selected.equals("today"));
        binding.chipWeek.setChecked(selected.equals("week"));
        binding.chipAll.setChecked(selected.equals("all"));
    }

    void applyFilters() {
        List<ScanFile> tempList = new ArrayList<>(scanFileList);

        // Appliquer le filtre de date
        tempList = filterByDate(tempList);

        // Appliquer le filtre de keywords
        if (filterByKeywords) {
            List<ScanFile> withKeywords = new ArrayList<>();
            for (ScanFile scan : tempList) {
                if (scan.keywords != null && !scan.keywords.isEmpty()) {
                    withKeywords.add(scan);
                }
            }
            tempList = withKeywords;
        }

        // Appliquer la recherche texte
        if (!currentQuery.isEmpty()) {
            tempList = filterByQuery(tempList, currentQuery);
        }

        filteredList.clear();
        filteredList.addAll(tempList);
        updateUI();
    }

    private List<ScanFile> filterByDate(List<ScanFile> list) {
        long startTime = 0;

        switch (currentDateFilter) {
            case "today":
                Calendar calToday = Calendar.getInstance();
                calToday.set(Calendar.HOUR_OF_DAY, 0);
                calToday.set(Calendar.MINUTE, 0);
                calToday.set(Calendar.SECOND, 0);
                calToday.set(Calendar.MILLISECOND, 0);
                startTime = calToday.getTimeInMillis();
                break;
            case "week":
                Calendar calWeek = Calendar.getInstance();
                calWeek.add(Calendar.DAY_OF_YEAR, -7);
                startTime = calWeek.getTimeInMillis();
                break;
            case "all":
                return list;
        }

        List<ScanFile> filtered = new ArrayList<>();
        for (ScanFile scan : list) {
            if (scan.time >= startTime) {
                filtered.add(scan);
            }
        }
        return filtered;
    }

    private List<ScanFile> filterByQuery(List<ScanFile> list, String query) {
        List<ScanFile> filtered = new ArrayList<>();
        String queryLower = query.toLowerCase();

        for (ScanFile scan : list) {
            boolean matches = false;
            if (scan.text != null && scan.text.toLowerCase().contains(queryLower)) {
                matches = true;
            } else if (scan.keywords != null && scan.keywords.toLowerCase().contains(queryLower)) {
                matches = true;
            }

            if (matches) {
                filtered.add(scan);
            }
        }
        return filtered;
    }

    @Override
    public boolean onQueryTextSubmit(String query) {
        currentQuery = query;
        applyFilters();
        return true;
    }

    @Override
    public boolean onQueryTextChange(String newText) {
        currentQuery = newText;
        applyFilters();
        return true;
    }

    public void OpenHistoryFile(int position) {
        if (position >= 0 && position < filteredList.size()) {
            Log.d(TAG, "OpenHistoryFile: " + position);
            Intent resultActivity = new Intent(this, ResultActivity.class);
            resultActivity.putExtra("text", filteredList.get(position).text);
            resultActivity.putExtra("uid", filteredList.get(position).uid);
            resultActivity.putExtra("ad2db", "no");
            startActivity(resultActivity);
        }
    }

    public void CopyText(int position) {
        if (position >= 0 && position < filteredList.size()) {
            ClipboardManager clipboard = (ClipboardManager) getSystemService(CLIPBOARD_SERVICE);
            ClipData clip = ClipData.newPlainText("OCR", filteredList.get(position).text);
            clipboard.setPrimaryClip(clip);
            Toast.makeText(this, "Copied to Clipboard", Toast.LENGTH_SHORT).show();
        }
    }

    public void ShareText(int position) {
        if (position >= 0 && position < filteredList.size()) {
            Intent sendIntent = new Intent();
            sendIntent.setAction(Intent.ACTION_SEND);
            sendIntent.putExtra(Intent.EXTRA_TEXT, filteredList.get(position).text);
            sendIntent.setType("text/plain");
            Intent shareIntent = Intent.createChooser(sendIntent, null);
            startActivity(shareIntent);
        }
    }
}
