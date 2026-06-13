package com.kartik.myschool.adapter;

import android.text.Editable;
import android.text.TextWatcher;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.EditText;
import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;
import com.kartik.myschool.databinding.ItemWeightageSubjectBinding;
import com.kartik.myschool.model.Subject;
import java.util.ArrayList;
import java.util.List;

public class DeclareWeightageAdapter extends RecyclerView.Adapter<DeclareWeightageAdapter.VH> {

    private final List<Subject> subjects = new ArrayList<>();

    public void setData(List<Subject> list) {
        subjects.clear();
        if (list != null) {
            subjects.addAll(list);
        }
        notifyDataSetChanged();
    }

    public List<Subject> getSubjectsList() {
        return subjects;
    }

    @NonNull
    @Override
    public VH onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        ItemWeightageSubjectBinding b = ItemWeightageSubjectBinding.inflate(
                LayoutInflater.from(parent.getContext()), parent, false);
        return new VH(b);
    }

    @Override
    public void onBindViewHolder(@NonNull VH holder, int position) {
        Subject subject = subjects.get(position);
        ItemWeightageSubjectBinding b = holder.b;

        // Set subject name
        b.tvSubjectName.setText(com.kartik.myschool.utils.pdf.PdfLocalizer.translateSubject(b.getRoot().getContext(), subject.name));

        // Subject Initial inside colored circle
        String initial = subject.name != null && !subject.name.isEmpty() 
                ? subject.name.substring(0, 1).toUpperCase() : "?";
        b.tvSubjectInitial.setText(initial);

        // Expand / collapse sub-fields layout
        b.layoutHeader.setOnClickListener(v -> {
            boolean visible = b.layoutSubFields.getVisibility() == View.VISIBLE;
            b.layoutSubFields.setVisibility(visible ? View.GONE : View.VISIBLE);
            b.viewHeaderDivider.setVisibility(visible ? View.GONE : View.VISIBLE);
            b.ivExpandArrow.setRotation(visible ? 0f : 180f);
        });

        // Initialize values into inputs
        b.etMaxMarks.setText(String.valueOf(subject.maxMarks));
        b.etNirikhshanMax.setText(String.valueOf(subject.maxNirikhshan));
        b.etTondiKamMax.setText(String.valueOf(subject.maxTondiKam));
        b.etPratyakshikMax.setText(String.valueOf(subject.maxPratyakshik));
        b.etUpkramMax.setText(String.valueOf(subject.maxUpkram));
        b.etPrakalpMax.setText(String.valueOf(subject.maxPrakalp));
        b.etChachaniMax.setText(String.valueOf(subject.maxChachani));
        b.etSwadhyayMax.setText(String.valueOf(subject.maxSwadhyay));
        b.etItarMax.setText(String.valueOf(subject.maxItar));
        b.etTondiMax.setText(String.valueOf(subject.maxTondi));
        b.etPratyakshikBMax.setText(String.valueOf(subject.maxPratyakshikB));
        b.etLekhiMax.setText(String.valueOf(subject.maxLekhi));

        // Initial breakdown set
        updateBreakdownText(b, subject.maxMarks);

        // Hide summative fields for specific subjects
        String sName = subject.name != null ? subject.name.toLowerCase() : "";
        boolean isFormativeOnly = sName.contains("drawing") || sName.contains("कला") ||
                                  sName.contains("work experi") || sName.contains("कार्यानुभव") ||
                                  sName.contains("physical") || sName.contains("शारीरिक") ||
                                  sName.contains("personality") || sName.contains("व्यक्तिमत्त्व");
                                  
        if (isFormativeOnly) {
            b.layoutSummative.setVisibility(View.GONE);
            // Force summative fields to 0
            subject.maxTondi = 0;
            subject.maxPratyakshikB = 0;
            subject.maxLekhi = 0;
            b.etTondiMax.setText("0");
            b.etPratyakshikBMax.setText("0");
            b.etLekhiMax.setText("0");
        } else {
            b.layoutSummative.setVisibility(View.VISIBLE);
        }

        // ── Main total watch ──────────────────────────────────────────────────
        b.etMaxMarks.addTextChangedListener(new TextWatcher() {
            @Override public void beforeTextChanged(CharSequence s, int start, int count, int after) {}
            @Override public void onTextChanged(CharSequence s, int start, int before, int count) {}
            @Override public void afterTextChanged(Editable s) {
                if (!b.etMaxMarks.hasFocus()) return;
                String valStr = s.toString().trim();
                if (valStr.isEmpty()) return;
                try {
                    int val = Integer.parseInt(valStr);
                    subject.maxMarks = val;
                    updateBreakdownText(b, val);
                    
                    boolean isFormativeOnly = subject.name != null && (
                            subject.name.toLowerCase().contains("drawing") || subject.name.contains("कला") ||
                            subject.name.toLowerCase().contains("work experi") || subject.name.contains("कार्यानुभव") ||
                            subject.name.toLowerCase().contains("physical") || subject.name.contains("शारीरिक") ||
                            subject.name.toLowerCase().contains("personality") || subject.name.contains("व्यक्तिमत्त्व")
                    );

                    // Auto-scale defaults for sub-fields
                    int akarikMax = isFormativeOnly ? val : val / 2;
                    int sanklitMax = isFormativeOnly ? 0 : val - akarikMax;

                    subject.maxNirikhshan   = 0;
                    subject.maxTondiKam     = akarikMax * 10 / 50;
                    subject.maxPratyakshik  = akarikMax * 10 / 50;
                    subject.maxUpkram       = akarikMax * 10 / 50;
                    subject.maxPrakalp      = 0;
                    subject.maxChachani     = akarikMax * 20 / 50;
                    subject.maxSwadhyay     = 0;
                    subject.maxItar         = 0;

                    subject.maxTondi        = sanklitMax * 10 / 50;
                    subject.maxPratyakshikB = sanklitMax * 10 / 50;
                    subject.maxLekhi        = sanklitMax - subject.maxTondi - subject.maxPratyakshikB;

                    // Update UI inputs
                    setVal(b.etNirikhshanMax, subject.maxNirikhshan);
                    setVal(b.etTondiKamMax, subject.maxTondiKam);
                    setVal(b.etPratyakshikMax, subject.maxPratyakshik);
                    setVal(b.etUpkramMax, subject.maxUpkram);
                    setVal(b.etPrakalpMax, subject.maxPrakalp);
                    setVal(b.etChachaniMax, subject.maxChachani);
                    setVal(b.etSwadhyayMax, subject.maxSwadhyay);
                    setVal(b.etItarMax, subject.maxItar);
                    setVal(b.etTondiMax, subject.maxTondi);
                    setVal(b.etPratyakshikBMax, subject.maxPratyakshikB);
                    setVal(b.etLekhiMax, subject.maxLekhi);

                } catch (NumberFormatException ignored) {}
            }
        });

        // ── Sub-fields watches ────────────────────────────────────────────────
        TextWatcher subWatcher = new TextWatcher() {
            @Override public void beforeTextChanged(CharSequence s, int start, int count, int after) {}
            @Override public void onTextChanged(CharSequence s, int start, int before, int count) {}
            @Override public void afterTextChanged(Editable s) {
                // Determine which input triggered and only execute if focused
                if (s.hashCode() == b.etNirikhshanMax.getText().hashCode() && !b.etNirikhshanMax.hasFocus()) return;
                if (s.hashCode() == b.etTondiKamMax.getText().hashCode() && !b.etTondiKamMax.hasFocus()) return;
                if (s.hashCode() == b.etPratyakshikMax.getText().hashCode() && !b.etPratyakshikMax.hasFocus()) return;
                if (s.hashCode() == b.etUpkramMax.getText().hashCode() && !b.etUpkramMax.hasFocus()) return;
                if (s.hashCode() == b.etPrakalpMax.getText().hashCode() && !b.etPrakalpMax.hasFocus()) return;
                if (s.hashCode() == b.etChachaniMax.getText().hashCode() && !b.etChachaniMax.hasFocus()) return;
                if (s.hashCode() == b.etSwadhyayMax.getText().hashCode() && !b.etSwadhyayMax.hasFocus()) return;
                if (s.hashCode() == b.etItarMax.getText().hashCode() && !b.etItarMax.hasFocus()) return;
                if (s.hashCode() == b.etTondiMax.getText().hashCode() && !b.etTondiMax.hasFocus()) return;
                if (s.hashCode() == b.etPratyakshikBMax.getText().hashCode() && !b.etPratyakshikBMax.hasFocus()) return;
                if (s.hashCode() == b.etLekhiMax.getText().hashCode() && !b.etLekhiMax.hasFocus()) return;

                // Sync current text values back to model
                subject.maxNirikhshan   = getInt(b.etNirikhshanMax);
                subject.maxTondiKam     = getInt(b.etTondiKamMax);
                subject.maxPratyakshik  = getInt(b.etPratyakshikMax);
                subject.maxUpkram       = getInt(b.etUpkramMax);
                subject.maxPrakalp      = getInt(b.etPrakalpMax);
                subject.maxChachani     = getInt(b.etChachaniMax);
                subject.maxSwadhyay     = getInt(b.etSwadhyayMax);
                subject.maxItar         = getInt(b.etItarMax);
                subject.maxTondi        = getInt(b.etTondiMax);
                subject.maxPratyakshikB = getInt(b.etPratyakshikBMax);
                subject.maxLekhi        = getInt(b.etLekhiMax);

                // Update total
                int total = subject.maxNirikhshan + subject.maxTondiKam + subject.maxPratyakshik 
                          + subject.maxUpkram + subject.maxPrakalp + subject.maxChachani 
                          + subject.maxSwadhyay + subject.maxItar + subject.maxTondi 
                          + subject.maxPratyakshikB + subject.maxLekhi;

                subject.maxMarks = total;
                setVal(b.etMaxMarks, total);
                updateBreakdownText(b, total);
            }
        };

        b.etNirikhshanMax.addTextChangedListener(subWatcher);
        b.etTondiKamMax.addTextChangedListener(subWatcher);
        b.etPratyakshikMax.addTextChangedListener(subWatcher);
        b.etUpkramMax.addTextChangedListener(subWatcher);
        b.etPrakalpMax.addTextChangedListener(subWatcher);
        b.etChachaniMax.addTextChangedListener(subWatcher);
        b.etSwadhyayMax.addTextChangedListener(subWatcher);
        b.etItarMax.addTextChangedListener(subWatcher);
        b.etTondiMax.addTextChangedListener(subWatcher);
        b.etPratyakshikBMax.addTextChangedListener(subWatcher);
        b.etLekhiMax.addTextChangedListener(subWatcher);
    }

    private void setVal(EditText et, int val) {
        et.setText(String.valueOf(val));
    }

    private int getInt(EditText et) {
        String valStr = et.getText() != null ? et.getText().toString().trim() : "";
        if (valStr.isEmpty()) return 0;
        try {
            return Integer.parseInt(valStr);
        } catch (NumberFormatException e) {
            return 0;
        }
    }

    private void updateBreakdownText(ItemWeightageSubjectBinding b, int maxMarks) {
        int akarikMax = getInt(b.etNirikhshanMax) + getInt(b.etTondiKamMax) + getInt(b.etPratyakshikMax)
                      + getInt(b.etUpkramMax) + getInt(b.etPrakalpMax) + getInt(b.etChachaniMax)
                      + getInt(b.etSwadhyayMax) + getInt(b.etItarMax);
        int sanklitMax = getInt(b.etTondiMax) + getInt(b.etPratyakshikBMax) + getInt(b.etLekhiMax);

        // If sub-fields are zero, show simple mathematical halves
        if (akarikMax == 0 && sanklitMax == 0) {
            akarikMax = maxMarks / 2;
            sanklitMax = maxMarks - akarikMax;
        }
        b.tvWeightageBreakdown.setText("आकारिक (Formative): " + akarikMax + " | संकलित (Summative): " + sanklitMax);
    }

    @Override
    public int getItemCount() {
        return subjects.size();
    }

    static class VH extends RecyclerView.ViewHolder {
        final ItemWeightageSubjectBinding b;
        VH(ItemWeightageSubjectBinding b) {
            super(b.getRoot());
            this.b = b;
        }
    }
}
