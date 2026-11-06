package pfm.android;

/** Single allocation policy for qualification, entrants and table legends. Cup places use league fallback. */
final class EuropeanPlaces {
    static boolean topFive(String code){return code.equals("en")||code.equals("es")||code.equals("it")||code.equals("de")||code.equals("fr");}
    static int champions(String code){return !topFive(code)?0:code.equals("en")||code.equals("es")||code.equals("it")?4:3;}
    static int europa(String code){return topFive(code)?3:0;}
    static boolean european(String competition){return competition.startsWith("ucl:")||competition.startsWith("uel:");}
    static String quotas(){return "BASE LEAGUE PLACES\nEngland / Spain / Italy: UCL 1–4 · UEL 5–7\nGermany / France: UCL 1–3 · UEL 4–6\nCup places use the league table until domestic cup qualification is available. Holders may shift individual places.";}
    static int zone(String league,int rank,int count){int cl=champions(league),el=europa(league),rel=league.equals("de")?2:topFive(league)?3:0;if(rank<=cl)return 0xffdcecff;if(rank<=cl+el)return 0xffffefc2;if(rel>0&&rank>count-rel)return 0xffffdddd;return 0;}
}
