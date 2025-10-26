package com.example.proyectopresionarterial;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.widget.SearchView;
import androidx.fragment.app.Fragment;
import androidx.lifecycle.ViewModelProvider;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import androidx.swiperefreshlayout.widget.SwipeRefreshLayout;

import com.google.android.material.button.MaterialButtonToggleGroup;
import com.google.android.material.chip.Chip;
import com.google.android.material.chip.ChipGroup;
import com.google.android.material.snackbar.Snackbar;

import java.util.ArrayList;
import java.util.List;

public class HistoryFragment extends Fragment implements HistoryAdapter.OnRecordClickListener {

    private HistoryViewModel viewModel;
    private RecyclerView rv;
    private TextView tvEmpty;
    private HistoryAdapter adapter;
    private SwipeRefreshLayout swipeRefreshLayout;
    private MaterialButtonToggleGroup toggleGroup;
    private ChipGroup chipGroupFilters;
    private SearchView searchView;
    private int filterTimeframe = HistoryViewModel.FILTER_ALL;
    private List<String> activeFilters = new ArrayList<>();

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        View v = inflater.inflate(R.layout.fragment_history, container, false);

        // Inicializar vistas
        rv = v.findViewById(R.id.rvHistory);
        tvEmpty = v.findViewById(R.id.tvEmptyHistory);
        swipeRefreshLayout = v.findViewById(R.id.swipeRefresh);
        toggleGroup = v.findViewById(R.id.toggleTimeframe);
        chipGroupFilters = v.findViewById(R.id.chipGroupFilters);
        searchView = v.findViewById(R.id.searchView);

        // Configurar RecyclerView
        rv.setLayoutManager(new LinearLayoutManager(requireContext()));
        adapter = new HistoryAdapter(this); // Usar constructor con listener
        rv.setAdapter(adapter);

        return v;
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        // Inicializar ViewModel
        viewModel = new ViewModelProvider(this).get(HistoryViewModel.class);

        // Configurar SwipeRefresh
        if (swipeRefreshLayout != null) {
            swipeRefreshLayout.setColorSchemeResources(
                    R.color.primaryColor,
                    R.color.secondaryColor);
            swipeRefreshLayout.setOnRefreshListener(this::loadData);
        }

        // Configurar toggleGroup para filtros temporales
        if (toggleGroup != null) {
            toggleGroup.addOnButtonCheckedListener((group, checkedId, isChecked) -> {
                if (isChecked) {
                    if (checkedId == R.id.btnFilterWeek) {
                        filterTimeframe = HistoryViewModel.FILTER_WEEK;
                    } else if (checkedId == R.id.btnFilterMonth) {
                        filterTimeframe = HistoryViewModel.FILTER_MONTH;
                    } else if (checkedId == R.id.btnFilterYear) {
                        filterTimeframe = HistoryViewModel.FILTER_YEAR;
                    } else {
                        filterTimeframe = HistoryViewModel.FILTER_ALL;
                    }
                    loadData();
                }
            });
        }

        // Configurar chips de filtro por condición
        if (chipGroupFilters != null) {
            chipGroupFilters.setOnCheckedStateChangeListener((group, checkedIds) -> {
                activeFilters.clear();
                for (int id : checkedIds) {
                    Chip chip = group.findViewById(id);
                    if (chip != null) {
                        activeFilters.add(chip.getText().toString().toLowerCase());
                    }
                }
                loadData();
            });
        }

        // Configurar búsqueda
        if (searchView != null) {
            searchView.setOnQueryTextListener(new SearchView.OnQueryTextListener() {
                @Override
                public boolean onQueryTextSubmit(String query) {
                    viewModel.setSearchQuery(query);
                    return true;
                }

                @Override
                public boolean onQueryTextChange(String newText) {
                    viewModel.setSearchQuery(newText);
                    return true;
                }
            });
        }

        // Observar cambios en los datos
        observeViewModel();

        // Cargar datos iniciales
        loadData();
    }

    private void observeViewModel() {
        viewModel.getRecords().observe(getViewLifecycleOwner(), records -> {
            if (swipeRefreshLayout != null && swipeRefreshLayout.isRefreshing()) {
                swipeRefreshLayout.setRefreshing(false);
            }

            if (records == null || records.isEmpty()) {
                showEmptyState();
            } else {
                hideEmptyState();
                adapter.submitList(records);
            }
        });

        viewModel.getError().observe(getViewLifecycleOwner(), error -> {
            if (error != null && !error.isEmpty()) {
                Snackbar.make(rv, error, Snackbar.LENGTH_LONG).show();
                if (swipeRefreshLayout != null && swipeRefreshLayout.isRefreshing()) {
                    swipeRefreshLayout.setRefreshing(false);
                }
            }
        });
    }

    private void loadData() {
        viewModel.loadRecords(filterTimeframe, activeFilters);
    }

    private void showEmptyState() {
        tvEmpty.setVisibility(View.VISIBLE);
        rv.setVisibility(View.GONE);
    }

    private void hideEmptyState() {
        tvEmpty.setVisibility(View.GONE);
        rv.setVisibility(View.VISIBLE);
    }

    @Override
    public void onRecordClick(BloodPressureRecord record) {
        // Implementar la lógica de clic en registro (puedes delegar a la Activity si es necesario)
        if (getActivity() instanceof HistoryActivity) {
            ((HistoryActivity) getActivity()).onRecordClick(record);
        }
    }

    /**
     * Método público para refrescar los datos del fragmento
     * Llamado desde la actividad contenedora
     */
    public void refreshData() {
        loadData();
    }
}
