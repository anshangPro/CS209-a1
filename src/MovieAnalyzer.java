import java.io.BufferedReader;
import java.io.FileReader;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Comparator;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;
import java.util.stream.Collectors;
import java.util.stream.Stream;

/**
 * the homework of CS209.
 */
public class MovieAnalyzer {

  ArrayList<Movie> movies = new ArrayList<>();

  /**
   * initialize the movie analyzer.
   *
   * @param datasetPath there the data stores
   *
   */
  public MovieAnalyzer(String datasetPath) {
    try {
      BufferedReader br = new BufferedReader(new FileReader(datasetPath));
      br.readLine();

      while (br.ready()) {
        String[] s = csvParse(br.readLine());
        Movie m = new Movie();
        m.posterLink = s[0];
        m.seriesTitle = s[1];
        m.releasedYear = Integer.parseInt(isNotNull(s[2]));
        m.certificate = s[3];
        m.runtime = s[4];
        m.runTimeInt = Integer.parseInt(isNotNull(s[4]).split(" ")[0]);
        m.genre = Arrays.stream(s[5].split(", *")).toList();
        m.imdbRating = Float.parseFloat(isNotNull(s[6]));
        m.overview = s[7];
        m.metaScore = Integer.parseInt(isNotNull(s[8]));
        m.director = s[9];
        m.stars.add(s[10]);
        m.stars.add(s[11]);
        m.stars.add(s[12]);
        m.stars.add(s[13]);
        m.noOfVotes = Integer.parseInt(isNotNull(s[14]));
        m.gross = Integer.parseInt(isNotNull(s[15]).replaceAll(",", ""));
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
        if (value.startsWith("\"") && !value.endsWith("\"")) {
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
      if (res[i] != null) {
        res[i] = res[i].replaceAll("#Truepoint#", "\"\"");
      }
    }
    return res;
  }

  /**
   * count movie by year.
   *
   * @return the movie count by Year
   */
  public Map<Integer, Integer> getMovieCountByYear() {
    Map<Integer, Integer> map = movies.stream()
        .sorted(Comparator.comparing(Movie::getReleasedYear).reversed())
        .collect(Collectors.toMap(
            movie -> movie.releasedYear, movie -> movie.releasedYear != -1 ? 1 : 0,
            Integer::sum, TreeMap::new
        ));
    Map<Integer, Integer> res = new TreeMap<>(Comparator.reverseOrder());
    res.putAll(map);
    return res;
  }

  /**
   * count movie by genre.
   *
   * @return the movie count by genre
   */
  public Map<String, Integer> getMovieCountByGenre() {
    Map<String, Integer> map = movies.stream().flatMap(movie -> movie.genre.stream())
        .collect(Collectors.toMap(g -> g, g -> 1, Integer::sum, TreeMap::new));
    Map<String, Integer> res = new LinkedHashMap<>();
    map.entrySet().stream().sorted(Map.Entry.comparingByValue(Comparator.reverseOrder()))
        .forEach(e -> res.put(e.getKey(), e.getValue()));
    return res;
  }

  /**
   * count the corporate times of stars.
   *
   * @return the corporate times of stars
   */
  public Map<List<String>, Integer> getCoStarCount() {
    List<List<String>> pairs = new ArrayList<>();
    movies.forEach(movie -> {
      pairs.add(starToList(movie.getStar1(), movie.getStar2()));
      pairs.add(starToList(movie.getStar1(), movie.getStar3()));
      pairs.add(starToList(movie.getStar1(), movie.getStar4()));
      pairs.add(starToList(movie.getStar2(), movie.getStar3()));
      pairs.add(starToList(movie.getStar2(), movie.getStar4()));
      pairs.add(starToList(movie.getStar3(), movie.getStar4()));
    });
    return pairs.stream().collect(Collectors.toMap(l -> l, l -> 1, Integer::sum));
  }

  private List<String> starToList(String star1, String star2) {
    List<String> li = new ArrayList<>();
    if (star1.compareTo(star2) <= 0) {
      li.add(star1);
      li.add(star2);
    } else {
      li.add(star2);
      li.add(star1);
    }
    return li;
  }

  /**
   * get the hottest movie list.
   *
   * @param topK how many movies should be in the list
   * @param by how to evaluate the hotness of movie
   *
   * @return list of movie name
   */
  public List<String> getTopMovies(int topK, String by) {
    Stream<Movie> m = movies.stream();
    if (by.equals("runtime")) {
      m = m.sorted(Comparator.comparing(Movie::getRunTimeInt).reversed()
          .thenComparing(m2 -> m2.seriesTitle));
    }
    if (by.equals("overview")) {
      m = m.sorted((m1, m2) -> {
        if (m1.overview.length() != m2.overview.length()) {
          return m2.overview.length() - m1.overview.length();
        } else {
          return m1.seriesTitle.compareTo(m2.seriesTitle);
        }
      });
    }
    return m.limit(topK).map(movie -> movie.seriesTitle).collect(Collectors.toList());
  }

  /**
   * get the hottest star list.
   *
   * @param topK how many stars should be in the list
   * @param by how to evaluate the hotness of star
   *
   * @return list of star name
   */
  public List<String> getTopStars(int topK, String by) {
    Map<String, List<Double>> starToRank = new HashMap<>();
    movies.forEach(m -> m.stars.forEach(s -> {
      starToRank.computeIfAbsent(s, k -> new ArrayList<>());
      if (by.equals("gross") && m.gross != -1) {
        starToRank.get(s).add((double) m.gross);
      }
    }));
    return starToRank.entrySet().stream().map(m -> {
      Rank r = new Rank();
      r.title = m.getKey();
      r.rank = m.getValue().stream().mapToDouble(Double::doubleValue).average().orElse(0);
      return r;
    }).sorted((s1, s2) -> {
      if (!s1.rank.equals(s2.rank)) {
        return s2.rank.compareTo(s1.rank);
      } else {
        return s1.title.compareTo(s2.title);
      }
    }).limit(topK).map(r -> r.title).toList();
  }

  /**
   * search movies.
   *
   * @param genre the genre of movie
   * @param minRating the minimum rate
   * @param maxRuntime the maximum runtime
   * @return a list of movies
   */
  public List<String> searchMovies(String genre, float minRating, int maxRuntime) {
    return movies.stream().filter(m -> m.genre.contains(genre)
            && m.imdbRating >= minRating && m.runTimeInt <= maxRuntime && m.runTimeInt > 0)
        .map(movie -> movie.seriesTitle).sorted(String::compareTo).collect(Collectors.toList());
  }

  private static class Movie {
    String posterLink;
    String seriesTitle;
    String certificate;
    String runtime;
    String overview;
    String director;
    int releasedYear;
    int metaScore;
    int noOfVotes;
    int gross;
    int runTimeInt;
    List<String> genre;
    List<String> stars = new ArrayList<>();
    double imdbRating;

    public String getSeriesTitle() {
      return seriesTitle;
    }

    public String getStar1() {
      return stars.get(0);
    }

    public String getStar2() {
      return stars.get(1);
    }

    public String getStar3() {
      return stars.get(2);
    }

    public String getStar4() {
      return stars.get(3);
    }

    public double getImdbRating() {
      return imdbRating;
    }

    public int getReleasedYear() {
      return releasedYear;
    }

    public int getOverview() {
      return overview.length();
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
        if (rank.compareTo(t.rank) != 0) {
          return -rank.compareTo(t.rank);
        }
        return title.compareTo(t.title);
      }
      return -1;
    }
  }

}