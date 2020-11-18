package org.dhis2.utils.customviews;

import android.Manifest;
import android.app.AlertDialog;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.net.Uri;
import android.provider.MediaStore;
import android.util.AttributeSet;
import android.view.View;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.cardview.widget.CardView;
import androidx.core.content.ContextCompat;
import androidx.core.content.FileProvider;
import androidx.databinding.DataBindingUtil;
import androidx.databinding.ViewDataBinding;
import androidx.fragment.app.FragmentActivity;
import androidx.fragment.app.FragmentManager;

import com.bumptech.glide.Glide;
import com.bumptech.glide.load.engine.DiskCacheStrategy;
import com.bumptech.glide.load.resource.bitmap.RoundedCorners;
import com.bumptech.glide.request.RequestOptions;
import com.google.android.material.button.MaterialButton;

import org.dhis2.Bindings.ExtensionsKt;
import org.dhis2.Bindings.FileExtensionsKt;
import org.dhis2.BuildConfig;
import org.dhis2.R;
import org.dhis2.data.forms.dataentry.fields.picture.PictureViewModel;
import org.dhis2.databinding.FormPictureAccentBinding;
import org.dhis2.databinding.FormPictureBinding;
import org.dhis2.utils.Constants;
import org.hisp.dhis.android.core.arch.helpers.FileResourceDirectoryHelper;

import java.io.File;

import kotlin.Pair;

import static android.text.TextUtils.isEmpty;

public class PictureView extends FieldLayout implements View.OnClickListener, View.OnFocusChangeListener {

    private ViewDataBinding binding;
    private String uid;
    private TextView errorView;
    private ImageView image;
    private MaterialButton addImageBtn;
    private String primaryUid;
    private Boolean isEditable;
    private ImageButton clearButton;
    private String currentValue;
    private CardView imageCard;
    private final FragmentManager supportFragmentManager = ((FragmentActivity) getContext()).getSupportFragmentManager();
    private PictureViewModel viewModel;

    public PictureView(Context context) {
        super(context);
        init(context);
    }

    public PictureView(Context context, AttributeSet attrs) {
        super(context, attrs);
        init(context);
    }

    public PictureView(Context context, AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
        init(context);
    }

    @Override
    public void onClick(View v) {
        if (isEditable && v == addImageBtn) {
            selectImage();
        }
    }

    @Override
    public void onFocusChange(View v, boolean hasFocus) {

    }

    private void setLayout() {
        if (isBgTransparent)
            binding = DataBindingUtil.inflate(inflater, R.layout.form_picture, this, true);
        else
            binding = DataBindingUtil.inflate(inflater, R.layout.form_picture_accent, this, true);

        errorView = findViewById(R.id.errorMessage);
        image = findViewById(R.id.image);
        image.setOnClickListener(view -> showFullPicture());
        addImageBtn = findViewById(R.id.addImageBtn);
        imageCard = findViewById(R.id.imageCard);
        addImageBtn.setOnClickListener(this);
        clearButton = findViewById(R.id.clear);
        clearButton.setOnClickListener(view -> {
                    if (isEditable && removeFile()) {
                        addImageBtn.setVisibility(VISIBLE);
                        imageCard.setVisibility(View.GONE);
                        Glide.with(this).clear(image);
                        viewModel.onClearValue();
                    }
                }
        );
    }

    private boolean removeFile() {
        return currentValue != null && new File(currentValue).delete();
    }

    public void setLabel(String label) {
        this.label = label;
        if (binding instanceof FormPictureBinding)
            ((FormPictureBinding) binding).setLabel(label);
        else
            ((FormPictureAccentBinding) binding).setLabel(label);
    }

    public void setDescription(String description) {
        if (binding instanceof FormPictureBinding)
            ((FormPictureBinding) binding).setDescription(description);
        else
            ((FormPictureAccentBinding) binding).setDescription(description);
    }

    public void setWarning(String msg) {
        if (!isEmpty(msg)) {
            errorView.setTextColor(ContextCompat.getColor(getContext(), R.color.warning_color));
            errorView.setText(msg);
            errorView.setVisibility(VISIBLE);
        } else
            errorView.setVisibility(GONE);
    }

    public void setError(String msg) {
        if (!isEmpty(msg)) {
            errorView.setTextColor(ContextCompat.getColor(getContext(), R.color.error_color));
            errorView.setText(msg);
            errorView.setVisibility(VISIBLE);
        } else
            errorView.setVisibility(GONE);
    }

    public void setIsBgTransparent(boolean isBgTransparent) {
        this.isBgTransparent = isBgTransparent;
        setLayout();
    }

    public void setProcessor(String primaryUid, String uid) {
        this.primaryUid = primaryUid;
        this.uid = uid;
    }

    public void setInitialValue(String value) {

        if (!isEmpty(value)) {

            Glide.with(image).clear(image);

            File file = new File(value);

            if (file.exists()) {
                Pair<Integer, Integer> dimensions = FileExtensionsKt.widthAndHeight(file, ExtensionsKt.getDp(200));
                currentValue = value;
                addImageBtn.setVisibility(GONE);
                imageCard.setVisibility(View.VISIBLE);
                Glide.with(image)
                        .load(file)
                        .apply(new RequestOptions().centerCrop())
                        .apply(RequestOptions.skipMemoryCacheOf(true))
                        .apply(RequestOptions.diskCacheStrategyOf(DiskCacheStrategy.NONE))
                        .apply(RequestOptions.bitmapTransform(new RoundedCorners(ExtensionsKt.getDp(6))))
                        .apply(RequestOptions.overrideOf(dimensions.component1(), dimensions.component2()))
                        .skipMemoryCache(true)
                        .into(image);
                clearButton.setVisibility(VISIBLE);
            }
        } else
            clearButton.setVisibility(View.GONE);
    }


    private void selectImage() {
        try {
            PackageManager pm = image.getContext().getPackageManager();
            int hasPerm = pm.checkPermission(Manifest.permission.CAMERA, image.getContext().getPackageName());
            if (hasPerm == PackageManager.PERMISSION_GRANTED) {
                final CharSequence[] options = {"Take Photo", "Choose From Gallery", "Cancel"};
                AlertDialog.Builder builder = new AlertDialog.Builder(image.getContext());
                builder.setTitle("Select Option");
                builder.setItems(options, (dialog, item) -> {
                    if (options[item].equals("Take Photo")) {
                        dialog.dismiss();
                        Intent intent = new Intent(MediaStore.ACTION_IMAGE_CAPTURE);
                        Uri photoUri = FileProvider.getUriForFile(getContext(),
                                BuildConfig.APPLICATION_ID + ".provider",
                                new File(FileResourceDirectoryHelper.getFileResourceDirectory(getContext()), "tempFile.png"));
                        intent.putExtra(MediaStore.EXTRA_OUTPUT, photoUri);
                        intent.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION);
                        ((FragmentActivity) getContext()).startActivityForResult(intent, Constants.CAMERA_REQUEST);
                    } else if (options[item].equals("Choose From Gallery")) {
                        dialog.dismiss();
                        Intent pickPhoto = new Intent(Intent.ACTION_PICK);
                        pickPhoto.putExtra("filename", primaryUid.concat("_").concat(uid));
                        pickPhoto.setType("image/*");
                        ((FragmentActivity) getContext()).startActivityForResult(pickPhoto, Constants.GALLERY_REQUEST);
                    } else if (options[item].equals("Cancel")) {
                        dialog.dismiss();
                    }
                });
                builder.show();
            } else
                Toast.makeText(image.getContext(), "Camera Permission error", Toast.LENGTH_SHORT).show();
        } catch (Exception e) {
            Toast.makeText(image.getContext(), "Camera Permission error", Toast.LENGTH_SHORT).show();
            e.printStackTrace();
        }
    }

    private void showFullPicture() {
        new ImageDetailBottomDialog(label, new File(currentValue))
                .show(supportFragmentManager, ImageDetailBottomDialog.TAG);
    }

    public void setEditable(Boolean editable) {
        isEditable = editable;
        addImageBtn.setEnabled(editable);
        setEditable(editable,
                findViewById(R.id.label),
                findViewById(R.id.descriptionLabel),
                addImageBtn,
                clearButton
        );
    }

    public void setViewModel(PictureViewModel viewModel) {
        this.viewModel = viewModel;
        setIsBgTransparent(viewModel.isBackgroundTransparent);
        setActivationListener(viewModel::onActivate);
        setProcessor(viewModel.uid().contains("_") ? viewModel.uid().split("_")[0] : viewModel.uid(),
                viewModel.uid().contains("_") ? viewModel.uid().split("_")[1] : viewModel.uid());
        setLabel(viewModel.getFormattedLabel());
        setDescription(viewModel.description());
        setInitialValue(viewModel.value());
        setEditable(viewModel.editable());
        setWarning(viewModel.warning());
        setError(viewModel.error());
    }

}
