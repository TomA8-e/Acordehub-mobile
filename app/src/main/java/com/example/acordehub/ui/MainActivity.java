package com.example.acordehub.ui;

import android.os.Bundle;

import androidx.appcompat.app.AppCompatActivity;
import androidx.navigation.NavController;
import androidx.navigation.NavDestination;
import androidx.navigation.NavOptions;
import androidx.navigation.fragment.NavHostFragment;

import com.example.acordehub.R;
import com.example.acordehub.databinding.ActivityMainBinding;

public class MainActivity extends AppCompatActivity {

    private ActivityMainBinding binding;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        binding = ActivityMainBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());

        // Configurar Navigation Component con Bottom Navigation
        NavHostFragment navHostFragment = (NavHostFragment) getSupportFragmentManager()
                .findFragmentById(R.id.nav_host_fragment);

        if (navHostFragment != null) {
            NavController navController = navHostFragment.getNavController();
            setupBottomNavigation(navController);
        }
    }

    private void setupBottomNavigation(NavController navController) {
        binding.bottomNavigation.setOnItemSelectedListener(item -> {
            NavDestination currentDestination = navController.getCurrentDestination();
            if (currentDestination != null && currentDestination.getId() == item.getItemId()) {
                return true;
            }

            NavOptions options = new NavOptions.Builder()
                    .setLaunchSingleTop(true)
                    .setRestoreState(true)
                    .setPopUpTo(navController.getGraph().getStartDestinationId(), false, true)
                    .build();

            try {
                navController.navigate(item.getItemId(), null, options);
                return true;
            } catch (IllegalArgumentException exception) {
                return false;
            }
        });

        navController.addOnDestinationChangedListener((controller, destination, arguments) -> {
            if (destination.getId() == R.id.homeFragment
                    || destination.getId() == R.id.perfilFragment
                    || destination.getId() == R.id.chatFragment) {
                binding.bottomNavigation.getMenu().findItem(destination.getId()).setChecked(true);
            }
        });
    }
}
