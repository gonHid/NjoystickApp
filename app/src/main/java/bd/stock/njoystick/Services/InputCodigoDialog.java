package bd.stock.njoystick.Services;

import android.app.AlertDialog;
import android.app.Dialog;
import android.content.Context;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.EditText;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.DialogFragment;

import bd.stock.njoystick.R;

public class InputCodigoDialog extends DialogFragment {

    public interface OnInputListener {
        void onInputReceived(String userInput);
    }

    private OnInputListener mListener;
    private EditText editText;

    @Override
    public void onAttach(@NonNull Context context) {
        super.onAttach(context);
        try {
            mListener = (OnInputListener) context;
        } catch (ClassCastException e) {
            throw new ClassCastException(context.toString() + " debe implementar OnInputListener");
        }
    }

    @NonNull
    @Override
    public Dialog onCreateDialog(@Nullable Bundle savedInstanceState) {
        AlertDialog.Builder builder = new AlertDialog.Builder(requireActivity());
        LayoutInflater inflater = requireActivity().getLayoutInflater();
        View view = inflater.inflate(R.layout.input_codigo_dialog, null);

        editText = view.findViewById(R.id.codigoInputManual);

        builder.setView(view)
                .setTitle("Ingrese un valor")
                .setPositiveButton("Aceptar", (dialog, which) -> {
                    String userInput = editText.getText().toString();
                    mListener.onInputReceived(userInput);
                })
                .setNegativeButton("Cancelar", (dialog, which) -> {
                    dismiss();
                });

        return builder.create();
    }
}