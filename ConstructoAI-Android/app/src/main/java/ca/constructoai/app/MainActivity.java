package ca.constructoai.app;

import android.Manifest;
import android.annotation.SuppressLint;
import android.app.Activity;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.graphics.Bitmap;
import android.net.Uri;
import android.os.Bundle;
import android.os.Environment;
import android.os.Handler;
import android.os.Looper;
import android.provider.MediaStore;
import android.view.View;
import android.view.WindowManager;
import android.webkit.GeolocationPermissions;
import android.webkit.PermissionRequest;
import android.webkit.ValueCallback;
import android.webkit.WebChromeClient;
import android.webkit.WebResourceError;
import android.webkit.WebResourceRequest;
import android.webkit.WebSettings;
import android.webkit.WebView;
import android.webkit.WebViewClient;
import android.widget.FrameLayout;
import android.webkit.JavascriptInterface;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;
import androidx.core.content.FileProvider;
import androidx.swiperefreshlayout.widget.SwipeRefreshLayout;

import java.io.File;
import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Locale;

public class MainActivity extends AppCompatActivity {

    private static final String WEB_URL = "https://constructoai.ca/";
    private static final int PERMISSION_REQUEST_CODE = 100;

    private WebView webView;
    private View splashScreen;
    private View errorScreen;
    private SwipeRefreshLayout swipeRefresh;
    private FrameLayout fullscreenContainer;
    private View customView;
    private WebChromeClient.CustomViewCallback customViewCallback;

    private ValueCallback<Uri[]> fileUploadCallback;
    private String cameraPhotoPath;

    private final ActivityResultLauncher<Intent> fileChooserLauncher = registerForActivityResult(
            new ActivityResultContracts.StartActivityForResult(),
            result -> {
                if (fileUploadCallback == null) return;

                Uri[] results = null;
                if (result.getResultCode() == Activity.RESULT_OK && result.getData() != null) {
                    String dataString = result.getData().getDataString();
                    if (dataString != null) {
                        results = new Uri[]{Uri.parse(dataString)};
                    }
                } else if (result.getResultCode() == Activity.RESULT_OK && cameraPhotoPath != null) {
                    results = new Uri[]{Uri.parse(cameraPhotoPath)};
                }

                fileUploadCallback.onReceiveValue(results);
                fileUploadCallback = null;
            }
    );

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        // Plein écran immersif
        getWindow().setFlags(
                WindowManager.LayoutParams.FLAG_LAYOUT_NO_LIMITS,
                WindowManager.LayoutParams.FLAG_LAYOUT_NO_LIMITS
        );

        initViews();
        checkPermissions();
        setupWebView();
        loadWebsite();
    }

    private void initViews() {
        webView = findViewById(R.id.webView);
        splashScreen = findViewById(R.id.splashScreen);
        errorScreen = findViewById(R.id.errorScreen);
        swipeRefresh = findViewById(R.id.swipeRefresh);
        fullscreenContainer = findViewById(R.id.fullscreenContainer);

        // Configuration du pull-to-refresh
        swipeRefresh.setColorSchemeColors(
                ContextCompat.getColor(this, R.color.primary)
        );
        swipeRefresh.setOnRefreshListener(() -> {
            webView.reload();
        });

        // Désactiver le swipe refresh par défaut - sera activé via JavaScript
        swipeRefresh.setEnabled(false);

        // Bouton retry sur l'écran d'erreur
        findViewById(R.id.btnRetry).setOnClickListener(v -> {
            errorScreen.setVisibility(View.GONE);
            splashScreen.setVisibility(View.VISIBLE);
            loadWebsite();
        });
    }

    // Interface JavaScript pour communiquer le scroll depuis la page web
    private class WebAppInterface {
        @JavascriptInterface
        public void setScrollPosition(int scrollY) {
            runOnUiThread(() -> {
                // Activer le pull-to-refresh UNIQUEMENT si on est tout en haut (scrollY <= 5)
                swipeRefresh.setEnabled(scrollY <= 5);
            });
        }
    }

    private void checkPermissions() {
        List<String> permissionsNeeded = new ArrayList<>();

        if (ContextCompat.checkSelfPermission(this, Manifest.permission.CAMERA)
                != PackageManager.PERMISSION_GRANTED) {
            permissionsNeeded.add(Manifest.permission.CAMERA);
        }
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION)
                != PackageManager.PERMISSION_GRANTED) {
            permissionsNeeded.add(Manifest.permission.ACCESS_FINE_LOCATION);
        }
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.RECORD_AUDIO)
                != PackageManager.PERMISSION_GRANTED) {
            permissionsNeeded.add(Manifest.permission.RECORD_AUDIO);
        }

        if (!permissionsNeeded.isEmpty()) {
            ActivityCompat.requestPermissions(this,
                    permissionsNeeded.toArray(new String[0]),
                    PERMISSION_REQUEST_CODE);
        }
    }

    @SuppressLint("SetJavaScriptEnabled")
    private void setupWebView() {
        WebSettings settings = webView.getSettings();

        // JavaScript et fonctionnalités web
        settings.setJavaScriptEnabled(true);
        settings.setDomStorageEnabled(true);
        settings.setDatabaseEnabled(true);
        settings.setAllowFileAccess(true);
        settings.setAllowContentAccess(true);

        // Cache
        settings.setCacheMode(WebSettings.LOAD_DEFAULT);

        // Zoom
        settings.setSupportZoom(true);
        settings.setBuiltInZoomControls(true);
        settings.setDisplayZoomControls(false);

        // Viewport et affichage
        settings.setUseWideViewPort(true);
        settings.setLoadWithOverviewMode(true);

        // Media
        settings.setMediaPlaybackRequiresUserGesture(false);

        // Géolocalisation
        settings.setGeolocationEnabled(true);

        // Mixed content (si nécessaire)
        settings.setMixedContentMode(WebSettings.MIXED_CONTENT_COMPATIBILITY_MODE);

        // User Agent personnalisé
        String userAgent = settings.getUserAgentString();
        settings.setUserAgentString(userAgent + " ConstructoAI-Android/1.0");

        // Ajouter l'interface JavaScript pour la communication
        webView.addJavascriptInterface(new WebAppInterface(), "AndroidApp");

        // WebViewClient pour la navigation
        webView.setWebViewClient(new WebViewClient() {
            @Override
            public void onPageStarted(WebView view, String url, Bitmap favicon) {
                super.onPageStarted(view, url, favicon);
                swipeRefresh.setRefreshing(true);
            }

            @Override
            public void onPageFinished(WebView view, String url) {
                super.onPageFinished(view, url);
                swipeRefresh.setRefreshing(false);
                hideSplashScreen();

                // Injecter le script de détection du scroll pour le pull-to-refresh
                injectScrollDetectionScript(view);
            }

            @Override
            public void onReceivedError(WebView view, WebResourceRequest request, WebResourceError error) {
                super.onReceivedError(view, request, error);
                if (request.isForMainFrame()) {
                    showErrorScreen();
                }
            }

            @Override
            public boolean shouldOverrideUrlLoading(WebView view, WebResourceRequest request) {
                String url = request.getUrl().toString();

                // Garder la navigation interne dans la WebView
                if (url.contains("constructoai.ca")) {
                    return false;
                }

                // Ouvrir les liens externes dans le navigateur
                if (url.startsWith("tel:") || url.startsWith("mailto:") ||
                        url.startsWith("sms:") || url.startsWith("whatsapp:")) {
                    Intent intent = new Intent(Intent.ACTION_VIEW, Uri.parse(url));
                    startActivity(intent);
                    return true;
                }

                // Autres liens externes
                Intent intent = new Intent(Intent.ACTION_VIEW, Uri.parse(url));
                startActivity(intent);
                return true;
            }
        });

        // WebChromeClient pour les fonctionnalités avancées
        webView.setWebChromeClient(new WebChromeClient() {

            // Gestion des permissions WebView (caméra, micro)
            @Override
            public void onPermissionRequest(final PermissionRequest request) {
                runOnUiThread(() -> request.grant(request.getResources()));
            }

            // Géolocalisation
            @Override
            public void onGeolocationPermissionsShowPrompt(String origin,
                                                           GeolocationPermissions.Callback callback) {
                callback.invoke(origin, true, false);
            }

            // Upload de fichiers
            @Override
            public boolean onShowFileChooser(WebView webView,
                                             ValueCallback<Uri[]> filePathCallback,
                                             FileChooserParams fileChooserParams) {
                if (fileUploadCallback != null) {
                    fileUploadCallback.onReceiveValue(null);
                }
                fileUploadCallback = filePathCallback;

                Intent takePictureIntent = new Intent(MediaStore.ACTION_IMAGE_CAPTURE);
                if (takePictureIntent.resolveActivity(getPackageManager()) != null) {
                    File photoFile = null;
                    try {
                        photoFile = createImageFile();
                    } catch (IOException ex) {
                        // Erreur création fichier
                    }
                    if (photoFile != null) {
                        cameraPhotoPath = "file:" + photoFile.getAbsolutePath();
                        Uri photoUri = FileProvider.getUriForFile(MainActivity.this,
                                getApplicationContext().getPackageName() + ".fileprovider",
                                photoFile);
                        takePictureIntent.putExtra(MediaStore.EXTRA_OUTPUT, photoUri);
                    } else {
                        takePictureIntent = null;
                    }
                }

                Intent contentSelectionIntent = new Intent(Intent.ACTION_GET_CONTENT);
                contentSelectionIntent.addCategory(Intent.CATEGORY_OPENABLE);
                contentSelectionIntent.setType("*/*");

                Intent chooserIntent = new Intent(Intent.ACTION_CHOOSER);
                chooserIntent.putExtra(Intent.EXTRA_INTENT, contentSelectionIntent);
                chooserIntent.putExtra(Intent.EXTRA_TITLE, "Sélectionner un fichier");

                if (takePictureIntent != null) {
                    chooserIntent.putExtra(Intent.EXTRA_INITIAL_INTENTS, new Intent[]{takePictureIntent});
                }

                fileChooserLauncher.launch(chooserIntent);
                return true;
            }

            // Plein écran pour les vidéos
            @Override
            public void onShowCustomView(View view, CustomViewCallback callback) {
                if (customView != null) {
                    callback.onCustomViewHidden();
                    return;
                }
                customView = view;
                customViewCallback = callback;
                fullscreenContainer.addView(view);
                fullscreenContainer.setVisibility(View.VISIBLE);
                webView.setVisibility(View.GONE);
            }

            @Override
            public void onHideCustomView() {
                if (customView == null) return;
                webView.setVisibility(View.VISIBLE);
                fullscreenContainer.setVisibility(View.GONE);
                fullscreenContainer.removeView(customView);
                customViewCallback.onCustomViewHidden();
                customView = null;
            }

            // Titre de la page
            @Override
            public void onReceivedTitle(WebView view, String title) {
                super.onReceivedTitle(view, title);
            }
        });
    }

    private File createImageFile() throws IOException {
        String timeStamp = new SimpleDateFormat("yyyyMMdd_HHmmss", Locale.getDefault()).format(new Date());
        String imageFileName = "JPEG_" + timeStamp + "_";
        File storageDir = getExternalFilesDir(Environment.DIRECTORY_PICTURES);
        return File.createTempFile(imageFileName, ".jpg", storageDir);
    }

    private void loadWebsite() {
        webView.loadUrl(WEB_URL);
    }

    private void injectScrollDetectionScript(WebView view) {
        // Script JavaScript qui surveille le scroll de la page
        // Fonctionne avec les pages normales ET les SPA (React, Vue, Angular)
        String script = "(function() {" +
                "  var lastScrollY = -1;" +
                "  function checkScroll() {" +
                "    var scrollY = window.pageYOffset || document.documentElement.scrollTop || document.body.scrollTop || 0;" +
                "    if (scrollY !== lastScrollY) {" +
                "      lastScrollY = scrollY;" +
                "      if (typeof AndroidApp !== 'undefined') {" +
                "        AndroidApp.setScrollPosition(Math.round(scrollY));" +
                "      }" +
                "    }" +
                "  }" +
                "  window.addEventListener('scroll', checkScroll, true);" +
                "  window.addEventListener('touchmove', checkScroll, true);" +
                "  document.addEventListener('scroll', checkScroll, true);" +
                "  checkScroll();" +
                "})();";
        view.evaluateJavascript(script, null);
    }

    private void hideSplashScreen() {
        new Handler(Looper.getMainLooper()).postDelayed(() -> {
            splashScreen.animate()
                    .alpha(0f)
                    .setDuration(300)
                    .withEndAction(() -> splashScreen.setVisibility(View.GONE))
                    .start();
        }, 500);
    }

    private void showErrorScreen() {
        splashScreen.setVisibility(View.GONE);
        errorScreen.setVisibility(View.VISIBLE);
    }

    @Override
    public void onBackPressed() {
        // Fermer le plein écran vidéo
        if (customView != null) {
            webView.getWebChromeClient().onHideCustomView();
            return;
        }

        // Navigation WebView
        if (webView.canGoBack()) {
            webView.goBack();
        } else {
            super.onBackPressed();
        }
    }

    @Override
    protected void onResume() {
        super.onResume();
        webView.onResume();
    }

    @Override
    protected void onPause() {
        super.onPause();
        webView.onPause();
    }

    @Override
    protected void onDestroy() {
        if (webView != null) {
            webView.destroy();
        }
        super.onDestroy();
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions,
                                           @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if (requestCode == PERMISSION_REQUEST_CODE) {
            for (int i = 0; i < permissions.length; i++) {
                if (grantResults[i] != PackageManager.PERMISSION_GRANTED) {
                    // Permission refusée, on continue quand même
                }
            }
        }
    }
}
