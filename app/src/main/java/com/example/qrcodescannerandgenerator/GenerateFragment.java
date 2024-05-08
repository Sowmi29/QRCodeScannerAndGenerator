package com.example.qrcodescannerandgenerator;

import android.content.ContentResolver;
import android.content.ContentValues;
import android.graphics.Bitmap;
import android.graphics.Bitmap.CompressFormat;
import android.graphics.drawable.BitmapDrawable;
import android.net.Uri;
import android.os.Bundle;
import android.os.StrictMode;
import android.provider.MediaStore;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.Toast;
import androidx.core.content.FileProvider;
import androidx.fragment.app.Fragment;
import com.google.zxing.BarcodeFormat;
import com.google.zxing.EncodeHintType;
import com.google.zxing.qrcode.QRCodeWriter;
import com.google.zxing.qrcode.decoder.ErrorCorrectionLevel;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.util.EnumMap;
import java.util.Map;

public class GenerateFragment extends Fragment {

    private EditText inputEditText;
    private ImageView qrCodeImageView;
    private Button generateButton, exportButton;
    private static final int QR_CODE_IMAGE_SIZE = 400;

    public GenerateFragment() {
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_generate, container, false);

        inputEditText = view.findViewById(R.id.edit_text_input);
        qrCodeImageView = view.findViewById(R.id.qr_code_image);
        generateButton = view.findViewById(R.id.generate_qr_code_button);
        exportButton = view.findViewById(R.id.export_button);

        generateButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                generateQRCode();
            }
        });

        exportButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                exportAsPNG();
            }
        });

        return view;
    }

    private void generateQRCode() {
        String inputText = inputEditText.getText().toString().trim();

        if (inputText.isEmpty()) {
            Toast.makeText(getActivity(), "Please enter text for QR code.", Toast.LENGTH_SHORT).show();
            return;
        }

        try {
            Map<EncodeHintType, Object> hints = new EnumMap<>(EncodeHintType.class);
            hints.put(EncodeHintType.CHARACTER_SET, "UTF-8");
            hints.put(EncodeHintType.ERROR_CORRECTION, ErrorCorrectionLevel.H);

            QRCodeWriter qrCodeWriter = new QRCodeWriter();
            com.google.zxing.common.BitMatrix bitMatrix = qrCodeWriter.encode(inputText, BarcodeFormat.QR_CODE, QR_CODE_IMAGE_SIZE, QR_CODE_IMAGE_SIZE, hints);

            int width = bitMatrix.getWidth();
            int height = bitMatrix.getHeight();
            Bitmap bitmap = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888);

            for (int x = 0; x < width; x++) {
                for (int y = 0; y < height; y++) {
                    bitmap.setPixel(x, y, bitMatrix.get(x, y) ? getResources().getColor(R.color.black) : getResources().getColor(R.color.white));
                }
            }

            qrCodeImageView.setImageBitmap(bitmap);
            qrCodeImageView.setVisibility(View.VISIBLE);
            exportButton.setVisibility(View.VISIBLE);
        } catch (Exception e) {
            e.printStackTrace();
            Toast.makeText(getActivity(), "Error generating QR code.", Toast.LENGTH_SHORT).show();
        }
    }


    private void exportAsPNG() {
        if (qrCodeImageView.getVisibility() != View.VISIBLE) {
            Toast.makeText(getActivity(), "No QR code to export.", Toast.LENGTH_SHORT).show();
            return;
        }

        Bitmap qrCodeBitmap = ((BitmapDrawable) qrCodeImageView.getDrawable()).getBitmap();

        ContentResolver resolver = requireContext().getContentResolver();

        ContentValues contentValues = new ContentValues();
        contentValues.put(MediaStore.Images.Media.DISPLAY_NAME, "QRCode.png");
        contentValues.put(MediaStore.Images.Media.MIME_TYPE, "image/png");
        contentValues.put(MediaStore.Images.Media.WIDTH, qrCodeBitmap.getWidth());
        contentValues.put(MediaStore.Images.Media.HEIGHT, qrCodeBitmap.getHeight());

        Uri imageUri = MediaStore.Images.Media.getContentUri(MediaStore.VOLUME_EXTERNAL_PRIMARY);

        try {
            Uri uri = ((ContentResolver) resolver).insert(imageUri, contentValues);
            if (uri != null) {
                try (OutputStream outputStream = resolver.openOutputStream(uri)) {
                    if (outputStream != null) {
                        qrCodeBitmap.compress(CompressFormat.PNG, 100, outputStream);
                        outputStream.flush();
                    }
                }

                resolver.update(uri, contentValues, null, null);

                Toast.makeText(getActivity(), "QR code saved as PNG.", Toast.LENGTH_SHORT).show();
            } else {
                Toast.makeText(getActivity(), "Error exporting QR code.", Toast.LENGTH_SHORT).show();
            }
        } catch (IOException e) {
            e.printStackTrace();
            Toast.makeText(getActivity(), "Error exporting QR code as PNG.", Toast.LENGTH_SHORT).show();
        }
    }
}