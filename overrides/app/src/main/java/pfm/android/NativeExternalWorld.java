package pfm.android;

import android.content.Context;
import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.HashMap;

/** Versioned, single-owner non-top-five player data. No match simulation here. */
final class NativeExternalWorld {
    private NativeExternalWorld() {}

    static void load(Context context, HashMap<String,ArrayList<NativeWorldCenter.T>> leagues) throws Exception {
        BufferedReader reader = new BufferedReader(new InputStreamReader(
                context.getAssets().open("europe_world_v74.tsv"), "UTF-8"));
        try {
            String line;
            reader.readLine();
            while ((line = reader.readLine()) != null) {
                String[] q = line.split("\t", -1);
                if (q.length != 20) throw new IllegalStateException("Invalid European row");
                String code = q[1];
                int index = Integer.parseInt(q[2]);
                ArrayList<NativeWorldCenter.T> teams = leagues.get(code);
                if (teams == null) { teams = new ArrayList<>(); leagues.put(code, teams); }
                if ("T".equals(q[0])) {
                    if (teams.size() != index) throw new IllegalStateException("European club order mismatch");
                    NativeWorldCenter.T team = new NativeWorldCenter.T();
                    team.id = index; team.name = q[3]; team.style = 55; team.form = 2;
                    team.activeFrom = Integer.parseInt(q[7]);
                    teams.add(team);
                } else if ("P".equals(q[0])) {
                    NativeWorldCenter.P p = new NativeWorldCenter.P();
                    p.id = Integer.parseInt(q[4]); p.name = q[5];
                    p.pos = Integer.parseInt(q[6]); p.age = Integer.parseInt(q[7]);
                    p.ovr = Integer.parseInt(q[8]); p.nat = q[9];
                    p.spe = Integer.parseInt(q[10]); p.res = Integer.parseInt(q[11]);
                    p.qua = Integer.parseInt(q[12]); p.mor = Integer.parseInt(q[13]);
                    p.value = Integer.parseInt(q[16]); p.wage = Integer.parseInt(q[17]);
                    p.uid = q[18]; p.provenance = q[19];
                    teams.get(index).ps.add(p);
                } else throw new IllegalStateException("Invalid European row type");
            }
        } finally { reader.close(); }
    }
}
