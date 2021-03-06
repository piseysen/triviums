package com.app.horizon.screens.main.home.stage.stages;

import android.arch.lifecycle.ViewModelProvider;
import android.arch.lifecycle.ViewModelProviders;
import android.databinding.DataBindingUtil;
import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentManager;
import android.support.v4.app.FragmentTransaction;
import android.support.v7.app.AlertDialog;
import android.support.v7.widget.GridLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.Window;
import android.view.WindowManager;
import android.widget.Button;
import android.widget.Toast;

import com.app.horizon.R;
import com.app.horizon.core.base.BaseFragment;
import com.app.horizon.databinding.FragmentStagesBinding;
import com.app.horizon.screens.main.home.stage.questions.QuestionFragment;
import com.app.horizon.utils.ConnectivityReceiver;
import com.app.horizon.utils.CountDownTimer;
import com.app.horizon.utils.Utils;

import java.util.ArrayList;
import java.util.List;

import javax.inject.Inject;

import io.reactivex.disposables.CompositeDisposable;

/**
 * A simple{@link Fragment } subclass.
 */


public class StagesFragment extends BaseFragment<StagesViewModel> {

    FragmentStagesBinding binding;
    StagesAdapter adapter;
    RecyclerView recyclerView;
    List<Integer> totalPage = new ArrayList<>();
    String categoryId, categoryName, stageProgress;
    int currentScore;
    Button button;
    CompositeDisposable disposable = new CompositeDisposable();

    public static final int MobileData = 2;
    public static final int WifiData = 1;
    boolean isConnected;
    @Inject
    ConnectivityReceiver connectivityReceiver;
    @Inject
    Utils utils;

    @Inject
    ViewModelProvider.Factory factory;
    private StagesViewModel viewModel;

    public StagesFragment() {
        // Required empty public constructor
    }


    @Override
    public StagesViewModel getViewModel() {
        viewModel = ViewModelProviders.of(this, factory).get(StagesViewModel.class);
        return viewModel;
    }


    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        // Inflate the layout for this fragment
        binding = DataBindingUtil.inflate(inflater, R.layout.fragment_stages, container,
                false);
        View view = binding.getRoot();
        binding.setClick(new MyHandler());

        //Get intent extras
        categoryId = getArguments().getString("CategoryId");
        categoryName = getArguments().getString("categoryName");

        //Initialize the recyclerview
        initRecyclerView();

        //Clear the adapter to avoid duplicates
        totalPage.clear();

        connectivityReceiver.observe(this, connectionModel -> {
                if (connectionModel.isConnected()) {
                    isConnected = true;
                    binding.noInternet.setVisibility(View.GONE);
                    binding.loader.setVisibility(View.VISIBLE);
                    binding.stageView.setVisibility(View.VISIBLE);
                    getProgress(categoryName);

                    //Call the showStage method
                    showStage(categoryId);
                } else {
                    isConnected = false;
                    showDialogForTimeout();
                }
        });

        return view;
    }

    private void initRecyclerView() {
        adapter = new StagesAdapter(getActivity(), totalPage, listener);
        recyclerView = binding.stageView;
        RecyclerView.LayoutManager layoutManager = new GridLayoutManager(getContext(), 2);
        recyclerView.setLayoutManager(layoutManager);
        recyclerView.setAdapter(adapter);
    }


    /**
     * Fetches stages of category
     *
     * @param categoryId
     */


    public void showStage(String categoryId) {
        viewModel.getStage(categoryId).observe(getViewLifecycleOwner(), response -> {
            binding.loader.setVisibility(View.GONE);

            if(response.getQuestionResponse() != null){
                int page = response.getQuestionResponse().getPaging().getTotalPages().intValue();
                totalPage.clear();
                for (int i = 1; i <= page; i++) {
                    totalPage.add(i);
                }
                adapter.updateStages(totalPage);
            }
        });
    }

    /**
     * Gets the progress of the user
     *
     * @param category
     */


    public void getProgress(String category) {
            viewModel.getProgressDetails(category).observe(getViewLifecycleOwner(), data -> {

                if (data.getData() != null) {
                    int score = Integer.parseInt(String.valueOf(data.getData().get("score")));
                    getCurrentScore(score);

                    int stageNumber = Integer.parseInt(data.getData().get("stageNumber").toString());
                    getStageNumber(String.valueOf(stageNumber));
                    adapter.updateButtonColor(stageNumber);
                }
            });
    }


    public String getStageNumber(String stageNum) {
        stageProgress = stageNum;
        return stageProgress;
    }

    public int getCurrentScore(int score) {
        currentScore = score;
        return currentScore;
    }


    public View.OnClickListener listener = view -> {

            //Check to confirm the instance of view i.e Button
            if (view instanceof Button) {
                button = (Button) view;

                //Checks to play stage 1 at default
                if (button.getText().toString().equals("1")) {
                    loadFragment();

                } else if (stageProgress == null) {
                    String message = String.valueOf(Integer.valueOf((String) button.getText()) - 1);
                    Log.e("Message", message);

                    Toast.makeText(getActivity(), "You must pass stage " +
                            message + " to continue ", Toast.LENGTH_SHORT).show();

                } else if (Integer.parseInt(String.valueOf(button.getText())) <= (Integer.parseInt(stageProgress) + 1)) {
                    loadFragment();
                } else {
                    String message = String.valueOf(Integer.valueOf((String) button.getText()) - 1);
                    Toast.makeText(getActivity(), "You must pass stage " +
                            message + " to continue ", Toast.LENGTH_SHORT).show();
                }
            } else {
                Log.e("View Instance:", "Error in getting the instance of view");
            }
    };

    public void loadFragment() {
        Fragment fragment = new QuestionFragment();
        FragmentManager fragmentManager = getActivity().getSupportFragmentManager();
        FragmentTransaction transaction = fragmentManager.beginTransaction();
        Bundle args = new Bundle();
        args.putString("categoryId", categoryId);
        args.putString("categoryName", categoryName);
        args.putString("stageNumber", String.valueOf(button.getText()));
        args.putInt("currentScore", currentScore);
        args.putInt("totalPages", totalPage.size());
        fragment.setArguments(args);

        transaction.replace(R.id.fragment_container, fragment)
                .addToBackStack(null)
                .commit();
    }

    private void showDialogForTimeout(){
        binding.loader.setVisibility(View.GONE);
        AlertDialog dialog;
        AlertDialog.Builder builder = new AlertDialog.Builder(getContext());
        builder.setMessage("Sorry, please check your internet connection")
                .setPositiveButton("OK", (dialog1, which) -> {
                    getActivity().finish();
                });
        dialog = builder.create();

        //This disables clicking outside the dialog box
        dialog.setCanceledOnTouchOutside(false);

        dialog.show();
    }

    @Override
    public void onResume() {
        super.onResume();

            if (isConnected) {
                binding.noInternet.setVisibility(View.GONE);
                binding.loader.setVisibility(View.VISIBLE);
                binding.stageView.setVisibility(View.VISIBLE);

                getProgress(categoryName);
                showStage(categoryId);
            } else {
               showDialogForTimeout();
            }
    }


    public class MyHandler {
        public void onButtonClick(View view) {
            getActivity().onBackPressed();
        }

    }


    @Override
    public void onDestroyView() {
        super.onDestroyView();
        disposable.dispose();
    }
}
