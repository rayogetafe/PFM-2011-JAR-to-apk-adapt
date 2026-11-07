package pfm.android;

/** Single allocation policy for qualification, entrants and table legends. Cup places use league fallback. */
final class EuropeanPlaces {
    static boolean topFive(String code){return code.equals("en")||code.equals("es")||code.equals("it")||code.equals("de")||code.equals("fr");}
    static String association(String league) {
        if (league.length() == 3 && league.charAt(2) == '2') {
            String country = league.substring(0, 2);
            if (topFive(country)) return country;
        }
        return league;
    }
    static int champions(String code){return !topFive(code)?0:code.equals("en")||code.equals("es")||code.equals("it")?4:3;}
    static int europa(String code){return topFive(code)?3:0;}
    static boolean european(String competition){return competition.startsWith("ucl:")||competition.startsWith("uel:")||competition.startsWith("uclq:");}
    static String disciplineCompetition(String competition){return competition.startsWith("uel:")?"ucl:"+competition.substring(4):competition.startsWith("uclq:")?"ucl:"+competition.substring(5):competition;}
    static String quotas(){return "BASE LEAGUE PLACES\nEngland / Spain / Italy: UCL groups 1–3 · UCL playoff 4 · UEL 5–6 + cup place\nGermany / France: UCL groups 1–2 · UCL playoff 3 · UEL 4–5 + cup place\nIf the cup winner enters the UCL, the eligible finalist takes the cup place. Any remaining vacancy goes to the next eligible league club. UCL playoff losers enter UEL groups. Frozen older tournaments keep their original groups. Holders may shift individual places.";}
    static int zone(String league,int rank,int count){int cl=champions(league),el=europa(league),rel=league.equals("de")?2:topFive(league)?3:0;if(rank<=cl)return 0xffdcecff;if(rank<=cl+el)return 0xffffefc2;if(rel>0&&rank>count-rel)return 0xffffdddd;return 0;}
}
