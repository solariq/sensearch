package tools.vector;

public class VectorImpl implements Vector {

    private final String word;
    private final double[] coordinates;

    public VectorImpl(String word, double[] coordinates) {
        this.word = word;
        this.coordinates = coordinates;
    }

    @Override
    public String getWord() {
        return word;
    }

    @Override
    public double[] getCoordinates() {
        return coordinates;
    }
}
