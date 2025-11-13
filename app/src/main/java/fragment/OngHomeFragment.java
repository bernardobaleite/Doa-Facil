package fragment;

import android.app.Activity;
import android.os.Bundle;
import androidx.fragment.app.Fragment;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.Window;

import androidx.core.content.ContextCompat;
import androidx.core.view.WindowInsetsControllerCompat;

import com.company.doafacil.R;
import activity.OngActivity;

// REBUILD: Implementing the user's vision for a yellow status bar on the home screen.
public class OngHomeFragment extends Fragment {

    private int originalStatusBarColor;
    private boolean originalLightStatusBar;

    public OngHomeFragment() {
        // Required empty public constructor
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        // Inflate the layout for this fragment
        return inflater.inflate(R.layout.fragment_ong_home, container, false);
    }

    @Override
    public void onResume() {
        super.onResume();
        Activity activity = getActivity();
        if (activity instanceof OngActivity) {
            ((OngActivity) activity).hideToolbar();

            Window window = activity.getWindow();
            if (window != null) {
                // Save original state
                originalStatusBarColor = window.getStatusBarColor();
                originalLightStatusBar = new WindowInsetsControllerCompat(window, window.getDecorView()).isAppearanceLightStatusBars();

                // Set new state for home screen
                window.setStatusBarColor(ContextCompat.getColor(requireContext(), R.color.yellow));
                new WindowInsetsControllerCompat(window, window.getDecorView()).setAppearanceLightStatusBars(true); // Dark icons on light background
            }
        }
    }

    @Override
    public void onPause() {
        super.onPause();
        Activity activity = getActivity();
        if (activity instanceof OngActivity) {
            ((OngActivity) activity).showToolbar();

            Window window = activity.getWindow();
            if (window != null) {
                // Restore original state when leaving
                window.setStatusBarColor(originalStatusBarColor);
                new WindowInsetsControllerCompat(window, window.getDecorView()).setAppearanceLightStatusBars(originalLightStatusBar);
            }
        }
    }
}
