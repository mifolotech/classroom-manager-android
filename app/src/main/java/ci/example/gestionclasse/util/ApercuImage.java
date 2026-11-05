package ci.example.gestionclasse.util;

import android.content.ContentResolver;
import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Matrix;
import android.media.ExifInterface;
import android.net.Uri;
import android.util.LruCache;
import android.view.View;
import android.widget.ImageView;

import java.io.InputStream;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import ci.example.gestionclasse.R;

// Charge la miniature d'une photo dans une ImageView, sans bloquer l'écran et sans saturer la mémoire.
public final class ApercuImage {

    // Deux threads d'arrière-plan pour décoder les images.
    private static final ExecutorService EXECUTEUR = Executors.newFixedThreadPool(2);

    // CACHE : garde en mémoire les miniatures déjà décodées, pour ne pas les recalculer à chaque défilement.
    // LruCache supprime automatiquement les moins récemment utilisées quand il est plein.
    private static final LruCache<String, Bitmap> CACHE;

    static {
        // On accorde au cache 1/8 de la mémoire maximale de l'application (calculée en kilo-octets).
        int memoireMaxKo = (int) (Runtime.getRuntime().maxMemory() / 1024);
        CACHE = new LruCache<String, Bitmap>(memoireMaxKo / 8) {
            @Override
            protected int sizeOf(String cle, Bitmap bitmap) {
                // Le cache compte la taille réelle de chaque image.
                return bitmap.getByteCount() / 1024;
            }
        };
    }

    private ApercuImage() {
    }

    // Affiche la miniature de la photo (uri) dans la vue. Peut être appelée à chaque affichage de ligne.
    public static void charger(ImageView vue, Uri uri) {
        String cle = uri.toString();

        // ÉTIQUETTE : la vue mémorise "quelle photo je dois afficher". Elle sert à détecter, plus bas,
        // qu'un résultat arrive trop tard pour une vue réutilisée par une autre ligne.
        vue.setTag(cle);

        // Déjà en cache : affichage immédiat.
        Bitmap enCache = CACHE.get(cle);
        if (enCache != null) {
            vue.setImageBitmap(enCache);
            return;
        }

        // Pas en cache : on vide la vue (elle a pu servir à une autre photo), puis on décode en arrière-plan.
        vue.setImageDrawable(null);

        // On récupère ces valeurs ICI, dans le thread de l'écran : les lire depuis l'arrière-plan serait risqué.
        Context contexte = vue.getContext().getApplicationContext();
        int largeurCible = vue.getResources().getDimensionPixelSize(R.dimen.apercu_largeur);
        int hauteurCible = vue.getResources().getDimensionPixelSize(R.dimen.apercu_hauteur);

        EXECUTEUR.execute(() -> {
            Bitmap bitmap = decoder(contexte, uri, largeurCible, hauteurCible);
            if (bitmap != null) {
                CACHE.put(cle, bitmap);
            }

            // On revient dans le thread de l'écran pour modifier la vue : c'est obligatoire.
            vue.post(() -> {
                // La vue a été réutilisée pour un autre message pendant le décodage : on ignore ce résultat.
                if (!cle.equals(vue.getTag())) {
                    return;
                }
                if (bitmap != null) {
                    vue.setImageBitmap(bitmap);
                } else {
                    // Fichier supprimé ou illisible : on cache la miniature, le lien reste disponible.
                    vue.setVisibility(View.GONE);
                }
            });
        });
    }

    // Lit la photo en la RÉDUISANT, puis la redresse si nécessaire. Renvoie null en cas d'échec.
    private static Bitmap decoder(Context contexte, Uri uri, int largeurCible, int hauteurCible) {
        ContentResolver resolver = contexte.getContentResolver();
        try {
            // 1re lecture : on demande seulement les DIMENSIONS, sans charger les pixels.
            BitmapFactory.Options options = new BitmapFactory.Options();
            options.inJustDecodeBounds = true;
            try (InputStream flux = resolver.openInputStream(uri)) {
                if (flux == null) {
                    return null;
                }
                BitmapFactory.decodeStream(flux, null, options);
            }
            if (options.outWidth <= 0 || options.outHeight <= 0) {
                return null;    // ce n'est pas une image lisible
            }

            // 2e lecture : on charge les pixels en réduisant l'image (inSampleSize : 2 = moitié moins large et haute).
            options.inSampleSize = calculerEchantillonnage(
                    options.outWidth, options.outHeight, largeurCible, hauteurCible);
            options.inJustDecodeBounds = false;

            Bitmap bitmap;
            try (InputStream flux = resolver.openInputStream(uri)) {
                if (flux == null) {
                    return null;
                }
                bitmap = BitmapFactory.decodeStream(flux, null, options);
            }
            if (bitmap == null) {
                return null;
            }

            // Les photos prises en portrait sont souvent stockées "couchées" avec une indication d'orientation (EXIF).
            int rotation = lireRotation(resolver, uri);
            if (rotation != 0) {
                Matrix matrice = new Matrix();
                matrice.postRotate(rotation);
                bitmap = Bitmap.createBitmap(
                        bitmap, 0, 0, bitmap.getWidth(), bitmap.getHeight(), matrice, true);
            }
            return bitmap;

        } catch (Exception | OutOfMemoryError e) {
            // Quoi qu'il arrive (fichier absent, image trop lourde...), on ne plante pas.
            return null;
        }
    }

    // Trouve le plus grand facteur de réduction (1, 2, 4, 8...) qui laisse l'image au moins aussi grande
    // que la zone d'affichage : on réduit beaucoup sans perdre en netteté.
    private static int calculerEchantillonnage(int largeur, int hauteur,
                                               int largeurCible, int hauteurCible) {
        int echantillon = 1;
        while ((largeur / (echantillon * 2)) >= largeurCible
                && (hauteur / (echantillon * 2)) >= hauteurCible) {
            echantillon *= 2;
        }
        return echantillon;
    }

    // Lit l'orientation EXIF de la photo et la convertit en degrés de rotation.
    private static int lireRotation(ContentResolver resolver, Uri uri) {
        try (InputStream flux = resolver.openInputStream(uri)) {
            if (flux == null) {
                return 0;
            }
            ExifInterface exif = new ExifInterface(flux);
            int orientation = exif.getAttributeInt(
                    ExifInterface.TAG_ORIENTATION, ExifInterface.ORIENTATION_NORMAL);
            switch (orientation) {
                case ExifInterface.ORIENTATION_ROTATE_90:
                    return 90;
                case ExifInterface.ORIENTATION_ROTATE_180:
                    return 180;
                case ExifInterface.ORIENTATION_ROTATE_270:
                    return 270;
                default:
                    return 0;
            }
        } catch (Exception e) {
            return 0;   // pas d'information d'orientation : on laisse l'image telle quelle
        }
    }
}