package com.example.myschool.adapter;

import android.view.LayoutInflater;
import android.view.ViewGroup;
import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.example.myschool.databinding.ItemReportCardBinding;

import java.util.ArrayList;
import java.util.List;

public class ReportPrintingAdapter extends RecyclerView.Adapter<ReportPrintingAdapter.VH> {

    public static class ReportTemplate {
        public String category;
        public String title;
        public String description;

        public ReportTemplate(String category, String title, String description) {
            this.category = category;
            this.title = title;
            this.description = description;
        }
    }

    public interface OnItemClickListener {
        void onItemClick(ReportTemplate template, int position);
    }

    private final List<ReportTemplate> items = new ArrayList<>();
    private OnItemClickListener listener;

    public ReportPrintingAdapter() {
        // नोंदवही (Register/Book) reports
        items.add(new ReportTemplate("नोंदवही",  "1. मुखपृष्ठ",                  "नोंदवहीचे आकर्षक मुखपृष्ठ"));
        items.add(new ReportTemplate("नोंदवही",  "2. अनुक्रमणिका",               "सत्र एक वा दोन अनुसार अनुक्रमणिका"));
        items.add(new ReportTemplate("नोंदवही",  "3. गुणनोंदी",                  "तंत्रे व श्रेणीसहित आकारिक-संकलित गुणनोंदी"));
        items.add(new ReportTemplate("नोंदवही",  "4. वर्णनात्मक नोंदी",          "सत्रानुसार विद्यार्थ्यांच्या वर्णनात्मक नोंदी"));
        items.add(new ReportTemplate("नोंदवही",  "5. श्रेणी तक्का",              "सत्र व विषयनुसार गुण व श्रेणी तक्का"));
        // निकालपत्रक (Result Sheet) reports
        items.add(new ReportTemplate("निकालपत्रक", "6. सर्वसामावेशक निकाल",     "आकारिक-संकलित गण श्रेणीयुक्त"));
        items.add(new ReportTemplate("निकालपत्रक", "7. श्रेणी तक्का",            "सत्र, वर्गवार मुले-मुली श्रेणी तक्का"));
        items.add(new ReportTemplate("निकालपत्रक", "8. गुण-श्रेणीयुक्त निकालपत्रक", "विषयवार एकूण गुण व श्रेणीयुक्त"));
        // गुणपत्रक (Marksheet) reports
        items.add(new ReportTemplate("गुणपत्रक",  "9. प्रगतीपत्रक मुखपृष्ठ",    "A4 साईज कलरफुल प्रगतीपत्रक"));
        items.add(new ReportTemplate("गुणपत्रक",  "10. प्रगतीपत्रक पृष्ठ",       "A4 साईज प्रगतीपत्रक आतील पृष्ठ"));
        // तक्के (Tables) reports
        items.add(new ReportTemplate("तक्के",     "11. उपयुक्त रिपोर्ट",         "विषयवार गुणतक्के"));
        // प्रगतीपुस्तक (Progress Book) reports
        items.add(new ReportTemplate("प्रगतीपुस्तक", "12. पाचवी आठवी गुणपत्रक", "इयत्ता पाचवी / आठवी गुणपत्रक"));
        // तक्के (Tables) reports continued
        items.add(new ReportTemplate("तक्के",     "13. पाचवी आठवी वार्षिक तक्के", "इयत्ता पाचवी / आठवी गुणतक्का"));
        // गुणपत्रक (Marksheet) reports continued
        items.add(new ReportTemplate("गुणपत्रक",  "14. प्रगतीपत्रक मुखपृष्ठ",   "A4 साईज कलरफुल प्रगतीपत्रक"));
        // निकालपत्रक (Result Sheet) continued
        items.add(new ReportTemplate("निकालपत्रक", "15. वार्षिक निकालपत्रक",     "सत्र व विषयनुसार गुण व श्रेणी तक्का"));
    }

    public void setOnItemClickListener(OnItemClickListener listener) {
        this.listener = listener;
    }

    @NonNull
    @Override
    public VH onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        ItemReportCardBinding b = ItemReportCardBinding.inflate(
                LayoutInflater.from(parent.getContext()), parent, false);
        return new VH(b);
    }

    @Override
    public void onBindViewHolder(@NonNull VH holder, int position) {
        ReportTemplate item = items.get(position);
        ItemReportCardBinding b = holder.b;

        b.tvReportCategory.setText(item.category);
        b.tvReportTitle.setText(item.title);
        b.tvReportDescription.setText(item.description);

        // Category colour-coding matches the screenshots
        int categoryColor;
        switch (item.category) {
            case "नोंदवही":       categoryColor = 0xFF5A4FCF; break; // indigo
            case "निकालपत्रक":  categoryColor = 0xFFD32F2F; break; // red
            case "गुणपत्रक":   categoryColor = 0xFF6A5ACD; break; // purple
            case "तक्के":      categoryColor = 0xFF757575; break; // grey
            case "प्रगतीपुस्तक": categoryColor = 0xFF388E3C; break; // green
            default:              categoryColor = 0xFF888888; break;
        }
        b.tvReportCategory.setTextColor(categoryColor);

        b.cardReportItem.setOnClickListener(v -> {
            if (listener != null) listener.onItemClick(item, position);
        });

        b.btnReportAction.setOnClickListener(v -> {
            if (listener != null) listener.onItemClick(item, position);
        });

        b.btnReportSettings.setOnClickListener(v -> {
            if (listener != null) listener.onItemClick(item, position);
        });
    }

    @Override
    public int getItemCount() {
        return items.size();
    }

    static class VH extends RecyclerView.ViewHolder {
        final ItemReportCardBinding b;
        VH(ItemReportCardBinding b) {
            super(b.getRoot());
            this.b = b;
        }
    }
}
