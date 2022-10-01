import java.io.BufferedReader;
import java.io.FileReader;
import java.io.IOException;
import java.util.*;
import java.util.stream.Collectors;
import java.util.stream.Stream;

public class MovieAnalyzer {

    public static void main(String[] args) {
        MovieAnalyzer ma = new MovieAnalyzer("resources/imdb_top_500.csv");
        var temp = ma.getTopStars(80, "gross");
        System.out.println();
        System.out.println("John Rhys-Davies".compareTo("Karen Allen"));
//        csvParse("\"https://m.media-amazon.com/images/M/MV5BOTI5ODc3NzExNV5BMl5BanBnXkFtZTcwNzYxNzQzMw@@._V1_UX67_CR0,0,67,98_AL_.jpg\",V for Vendetta,2005,A,132 min,\"Action, Drama, Sci-Fi\",8.2,\"In a future British tyranny, a shadowy freedom fighter, known only by the alias of \"\"V\"\", plots to overthrow it with the help of a young woman.\",62,James McTeigue,Hugo Weaving,Natalie Portman,Rupert Graves,Stephen Rea,1032749,\"70,511,035\"");
    }

    ArrayList<Movie> movies = new ArrayList<>();

    public MovieAnalyzer(String dataset_path) {
        try {
            BufferedReader br = new BufferedReader(new FileReader(dataset_path));
            br.readLine();
            boolean con = false;

            while (br.ready()) {
                String[] s = csvParse(br.readLine());
                Movie m = new Movie();
                m.Poster_Link = s[0];
                m.Series_Title = s[1];
                m.Released_Year = Integer.parseInt(isNotNull(s[2]));
                m.Certificate = s[3];
                m.Runtime = s[4];
                m.runTimeInt = Integer.parseInt(isNotNull(s[4]).split(" ")[0]);
                m.Genre = Arrays.stream(s[5].split(", *")).toList();
                m.IMDB_Rating = Double.parseDouble(isNotNull(s[6]));
                m.Overview = s[7];
                m.Meta_score = Integer.parseInt(isNotNull(s[8]));
                m.Director = s[9];
                m.Stars.add(s[10]);
                m.Stars.add(s[11]);
                m.Stars.add(s[12]);
                m.Stars.add(s[13]);
                m.No_of_Votes = Integer.parseInt(isNotNull(s[14]));
                m.Gross = Integer.parseInt(isNotNull(s[15]).replaceAll(",", ""));
                movies.add(m);
            }
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    private String isNotNull(String s) {
        return s == null || s.equals("") ? "-1" : s;
    }

    private static String[] csvParse(String s) {
        s = s.replaceAll("\"\"", "#Truepoint#");
        String[] tmp = s.split(",");
        String[] res = new String[16];
        int index = 0;
        boolean con = false;
        for (String value : tmp) {
            if (con) {
                if (value.endsWith("\"")) {
                    res[index++] += ',' + value.substring(0, value.length() - 1);
                    con = false;
                } else {
                    res[index] += ',' + value;
                }
            } else {
                if (value.startsWith("\"")&& !value.endsWith("\"")) {
                    con = true;
                    res[index] = value.substring(1);
                } else if (value.startsWith("\"") && value.endsWith("\"")) {
                    res[index++] = value.substring(1, value.length() - 1);
                } else {
                    res[index++] = value;
                }
            }
        }
        for (int i = 0; i < res.length; i++) {
            if (res[i] != null)
                res[i] = res[i].replaceAll("#Truepoint#", "\"");
        }
        return res;
    }

    public Map<Integer, Integer> getMovieCountByYear() {
        Map<Integer, Integer> map = movies.stream().sorted(Comparator.comparing(Movie::getReleased_Year).reversed()).collect(Collectors.toMap(movie -> movie.Released_Year, movie -> movie.Released_Year != -1 ? 1 : 0, Integer::sum, TreeMap::new));
        Map<Integer, Integer> res = new TreeMap<>(new Comparator<Integer>() {
            @Override
            public int compare(Integer o1, Integer o2) {
                return o2.compareTo(o1);
            }
        });
        res.putAll(map);
        return res;
    }

    public Map<String, Integer> getMovieCountByGenre() {
        return movies.stream().flatMap(movie -> movie.Genre.stream()).collect(Collectors.toMap(g -> g, g -> 1, Integer::sum, TreeMap::new));
    }

    public Map<List<String>, Integer> getCoStarCount() {
        return movies.stream().collect(Collectors.toMap(movie -> {
            List<String> li = new ArrayList<>();
            if (movie.getStar1().compareTo(movie.getStar2()) <= 0) {
                li.add(movie.getStar1());
                li.add(movie.getStar2());
            } else {
                li.add(movie.getStar2());
                li.add(movie.getStar1());
            }
            return li;
        }, movie -> 1, Integer::sum));
    }

    public List<String> getTopMovies(int top_k, String by) {
        Stream<Movie> m = movies.stream();
        if (by.equals("runtime"))
            m = m.sorted(Comparator.comparing(Movie::getRunTimeInt).reversed().thenComparing(m2 -> m2.Series_Title));
        if (by.equals("overview"))
            m = m.sorted(Comparator.comparing(Movie::getOverview).reversed().thenComparing(m2 -> m2.Series_Title));
        return m.limit(top_k).map(movie -> movie.Series_Title).collect(Collectors.toList());
    }

    public List<String> getTopStars(int top_k, String by) {
        Map<String, List<Double>> starToRank = new HashMap<>();
        movies.forEach(m -> {
            m.Stars.forEach(s -> {
                        starToRank.computeIfAbsent(s, k -> new ArrayList<>());
                        if (by.equals("rating") && m.IMDB_Rating != -1)
                            starToRank.get(s).add(m.IMDB_Rating);
                        if (by.equals("gross") && m.Gross != -1)
                            starToRank.get(s).add((double) m.Gross);
                    }
            );
        });
        return starToRank.entrySet().stream().map(m -> {
            Rank r = new Rank();
            r.title = m.getKey();
            r.rank = m.getValue().stream().mapToDouble(Double::doubleValue).average().orElse(0);
            return r;
        }).sorted(Rank::compareTo).limit(top_k).map(r -> r.title).toList();
        //return avgRank.entrySet().stream().sorted(Map.Entry.comparingByValue(Comparator.reverseOrder()).thenComparing(Map.Entry::getKey)).limit(top_k).map(Map.Entry::getKey).toList();
        //return avgRank.entrySet().stream().sorted(Comparator.comparing(Map.Entry::getValue)).limit(top_k).map(Map.Entry::getKey).toList();
    }

    public List<String> searchMovies(String genre, float min_rating, int max_runtime) {
        return movies.stream().filter(m -> m.Genre.contains(genre) && m.IMDB_Rating > min_rating && m.runTimeInt < max_runtime)
                .map(movie -> movie.Series_Title).collect(Collectors.toList());
    }

    private static class Movie {
        String Poster_Link, Series_Title, Certificate, Runtime, Overview, Director;
        int Released_Year, Meta_score, No_of_Votes, Gross, runTimeInt;
        List<String> Genre;
        List<String> Stars = new ArrayList<>();
        double IMDB_Rating;

        public String getStar1() {
            return Stars.get(0);
        }

        public String getStar2() {
            return Stars.get(1);
        }

        public String getStar3() {
            return Stars.get(2);
        }

        public String getStar4() {
            return Stars.get(3);
        }

        public double getIMDB_Rating() {
            return IMDB_Rating;
        }

        public int getReleased_Year() {
            return Released_Year;
        }

        public int getOverview() {
            return Overview.length();
        }

        public int getRunTimeInt() {
            return runTimeInt;
        }
    }

    private static class Rank implements Comparable {
        String title;
        Double rank;

        @Override
        public int compareTo(Object r) {
            if (r instanceof Rank t) {
                if (rank.compareTo(t.rank) != 0) return -rank.compareTo(t.rank);
                return title.compareTo(t.title);
            }
            return -1;
        }
    }

}