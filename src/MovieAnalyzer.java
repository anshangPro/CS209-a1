import java.io.BufferedReader;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.IOException;
import java.util.*;
import java.util.regex.*;
import java.util.stream.Collectors;
import java.util.stream.Stream;

public class MovieAnalyzer {

    public static void main(String[] args) {
        MovieAnalyzer ma = new MovieAnalyzer("resources/imdb_top_500.csv");
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
                for (String s1 : s) {
                    if (s1 == null || s1.equals("")) con = true;
                }
                if (con) {
                    con = false;
                    continue;
                }
                Movie m = new Movie();
                m.Poster_Link = s[0];
                m.Series_Title = s[1];
                m.Released_Year = Integer.parseInt(s[2]);
                m.Certificate = s[3];
                m.Runtime = s[4];
                m.runTimeInt = Integer.parseInt(s[4].split(" ")[0]);
                m.Genre = s[5];
                m.IMDB_Rating = Double.parseDouble(s[6]);
                m.Overview = s[7];
                m.Meta_score = Integer.parseInt(s[8]);
                m.Director = s[9];
                m.Star1 = s[10];
                m.Star2 = s[11];
                m.Star3 = s[12];
                m.Star4 = s[13];
                m.No_of_Votes = Integer.parseInt(s[14]);
                m.Gross = Integer.parseInt(s[15].replaceAll(",", ""));
                movies.add(m);
            }
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    private static String[] csvParse(String s) {
        String[] tmp = s.split(",");
        String[] res = new String[16];
        int index = 0;
        boolean con = false;
        for (String value : tmp) {
            if (con) {
                if (value.endsWith("\"") && !value.endsWith("\"\"")) {
                    res[index++] += ',' + value.substring(0, value.length() - 1);
                    con = false;
                } else {
                    res[index] += ',' + value;
                }
            } else {
                if (value.startsWith("\"") && !value.startsWith("\"\"") && !value.endsWith("\"")) {
                    con = true;
                    res[index] = value.substring(1);
                } else if (value.startsWith("\"") && !value.startsWith("\"\"") && value.endsWith("\"")) {
                    res[index++] = value.substring(1, value.length() - 1);
                } else {
                    res[index++] = value;
                }
            }
        }
        for (int i = 0; i < res.length; i++) {
            if (res[i] != null)
                res[i] = res[i].replaceAll("\"\"", "\"");
        }
        return res;
    }

    public Map<Integer, Integer> getMovieCountByYear() {
        return movies.stream().collect(Collectors.toMap(movie -> movie.Released_Year, movie -> 1, Integer::sum, TreeMap::new));
    }

    public Map<String, Integer> getMovieCountByGenre() {
        return movies.stream().collect(Collectors.toMap(movie -> movie.Genre, movie -> 1, Integer::sum, TreeMap::new));
    }

    public Map<List<String>, Integer> getCoStarCount() {
        return movies.stream().collect(Collectors.toMap(movie -> {
            List<String> li = new ArrayList<>();
            if (movie.Star1.compareTo(movie.Star2) <= 0) {
                li.add(movie.Star1);
                li.add(movie.Star2);
            } else {
                li.add(movie.Star2);
                li.add(movie.Star1);
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

    private class Movie {
        String Poster_Link, Series_Title, Certificate, Runtime, Genre, Overview, Director, Star1, Star2, Star3, Star4;
        int Released_Year, Meta_score, No_of_Votes, Gross, runTimeInt;
        double IMDB_Rating;

        public int getOverview() {
            return Overview.length();
        }

        public int getRunTimeInt() {
            return runTimeInt;
        }
    }

}