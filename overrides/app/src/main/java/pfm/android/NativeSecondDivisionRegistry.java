package pfm.android;

import android.app.Activity;
import android.app.AlertDialog;
import android.graphics.BitmapFactory;
import android.view.View;
import android.view.ViewGroup;
import android.view.Gravity;
import android.widget.*;
import java.io.*;
import java.util.*;

/** Read-only 2010/11 club data, separate from clubs with active career squads. */
final class NativeSecondDivisionRegistry {
    private static final String[] CODES = {"en2", "es2", "it2", "de2", "fr2"};
    private static final String[] NAMES = {"Championship", "Segunda División", "Serie B", "2. Bundesliga", "Ligue 2"};

    static void show(Activity activity) {
        Map<String, List<String[]>> clubs = new HashMap<>();
        for (String code : CODES) clubs.put(code, new ArrayList<>());
        try (BufferedReader in = new BufferedReader(new InputStreamReader(
                activity.getAssets().open("second_divisions_2010.tsv"), "UTF-8"))) {
            in.readLine();
            for (String line; (line = in.readLine()) != null;) {
                String[] club = line.split("\t", -1);
                if (club.length != 6 || !clubs.containsKey(club[1])) throw new IOException("Invalid club record");
                clubs.get(club[1]).add(club);
            }
        } catch (IOException error) {
            Toast.makeText(activity, "Second-division club data unavailable", Toast.LENGTH_SHORT).show();
            return;
        }

        LinearLayout root = new LinearLayout(activity);
        root.setOrientation(LinearLayout.VERTICAL);
        Spinner division = new Spinner(activity);
        division.setAdapter(new ArrayAdapter<>(activity, android.R.layout.simple_spinner_dropdown_item, NAMES));
        root.addView(division);
        TextView status = new TextView(activity);
        status.setPadding(dp(activity, 12), dp(activity, 8), dp(activity, 12), dp(activity, 8));
        root.addView(status);
        ListView list = new ListView(activity);
        root.addView(list, new LinearLayout.LayoutParams(-1, 0, 1));
        division.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
                List<String[]> rows = clubs.get(CODES[position]);
                status.setText(rows.size() + " clubs · 2010/11 club directory\nCareer squads and fixtures pending.");
                list.setAdapter(new ArrayAdapter<String[]>(activity, android.R.layout.simple_list_item_1, rows) {
                    @Override public View getView(int index, View convertView, ViewGroup parent) {
                        LinearLayout row = (LinearLayout) convertView;
                        if (row == null) {
                            row = new LinearLayout(activity);
                            row.setGravity(Gravity.CENTER_VERTICAL);
                            row.setPadding(dp(activity, 12), dp(activity, 4), dp(activity, 12), dp(activity, 4));
                            ImageView badge = new ImageView(activity);
                            badge.setScaleType(ImageView.ScaleType.FIT_CENTER);
                            row.addView(badge, new LinearLayout.LayoutParams(dp(activity, 48), dp(activity, 48)));
                            TextView name = new TextView(activity);
                            name.setTextSize(16);
                            name.setPadding(dp(activity, 12), 0, 0, 0);
                            row.addView(name);
                        }
                        String[] club = getItem(index);
                        ImageView badge = (ImageView) row.getChildAt(0);
                        badge.setImageDrawable(null);
                        try (InputStream in = activity.getAssets().open("second_division_badges/" + club[4])) {
                            badge.setImageBitmap(BitmapFactory.decodeStream(in));
                        } catch (IOException ignored) {}
                        ((TextView) row.getChildAt(1)).setText(club[2] + (club[3].isEmpty() ? "" : " · reserve team"));
                        return row;
                    }
                });
            }
            public void onNothingSelected(AdapterView<?> parent) {}
        });
        AlertDialog dialog = new AlertDialog.Builder(activity).setTitle("Second divisions · 2010/11")
                .setView(root).setPositiveButton("Close", null).create();
        dialog.setOnShowListener(v -> {
            if (dialog.getWindow() != null) {
                dialog.getWindow().setLayout(-1, Math.round(activity.getResources().getDisplayMetrics().heightPixels * .88f));
            }
        });
        dialog.show();
    }

    private static int dp(Activity activity, int value) {
        return Math.round(value * activity.getResources().getDisplayMetrics().density);
    }
}
