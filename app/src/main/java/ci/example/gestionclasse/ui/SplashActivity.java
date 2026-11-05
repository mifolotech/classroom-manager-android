package ci.example.gestionclasse.ui;

import android.content.Intent;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.view.animation.OvershootInterpolator;

import androidx.appcompat.app.AppCompatActivity;

import ci.example.gestionclasse.databinding.ActivitySplashBinding;

// Écran de démarrage : le logo apparaît en douceur, puis l'écran de choix du rôle s'ouvre tout seul.
public class SplashActivity extends AppCompatActivity {

    // Durée d'affichage de l'écran de démarrage, en millisecondes.
    private static final long DUREE_MS = 2300;

    private ActivitySplashBinding binding;

    // Handler : permet de "programmer" une action pour plus tard, dans le thread de l'écran.
    private final Handler handler = new Handler(Looper.getMainLooper());

    // L'action programmée : ouvrir l'écran de choix du rôle.
    private final Runnable ouvrirRole = this::ouvrirRole;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        binding = ActivitySplashBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());

        animer();
    }

    // On programme l'ouverture quand l'écran devient visible...
    @Override
    protected void onStart() {
        super.onStart();
        handler.postDelayed(ouvrirRole, DUREE_MS);
    }

    // ...et on l'annule si l'écran disparaît avant la fin (l'utilisateur quitte l'application, par exemple).
    @Override
    protected void onStop() {
        super.onStop();
        handler.removeCallbacks(ouvrirRole);
    }

    // Fait apparaître les éléments les uns après les autres.
    private void animer() {
        // Le logo grossit légèrement en apparaissant ; OvershootInterpolator donne un petit "rebond".
        binding.ivLogo.setScaleX(0.8f);
        binding.ivLogo.setScaleY(0.8f);
        binding.ivLogo.animate()
                .alpha(1f).scaleX(1f).scaleY(1f)
                .setDuration(800)
                .setInterpolator(new OvershootInterpolator(1.2f))
                .start();

        // Le nom et le slogan montent de 24dp en apparaissant, avec un petit décalage entre eux.
        float decalage = 24 * getResources().getDisplayMetrics().density;

        binding.tvNom.setTranslationY(decalage);
        binding.tvNom.animate()
                .alpha(1f).translationY(0f)
                .setStartDelay(350).setDuration(600)
                .start();

        binding.tvSlogan.setTranslationY(decalage);
        binding.tvSlogan.animate()
                .alpha(1f).translationY(0f)
                .setStartDelay(550).setDuration(600)
                .start();

        // La barre de chargement apparaît en dernier.
        binding.progress.animate()
                .alpha(1f)
                .setStartDelay(800).setDuration(500)
                .start();
    }

    // Ouvre le choix du rôle, puis ferme l'écran de démarrage : le bouton Retour ne reviendra donc pas ici.
    private void ouvrirRole() {
        startActivity(new Intent(SplashActivity.this, RoleActivity.class));
        finish();
    }
}