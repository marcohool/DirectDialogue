package Messages;

public class Query {

    private final QueryDescriptor queryDescriptor;
    private final String queryContent;


    public Query(String query) {
        String[] split = query.split(" ");

        this.queryDescriptor = QueryDescriptor.valueOf(split[0]);
        this.queryContent = split[1].toLowerCase();
    }

    public Query(QueryDescriptor queryDescriptor, String queryContent) {
        this.queryDescriptor = queryDescriptor;
        this.queryContent = queryContent.toLowerCase();
    }

    public QueryDescriptor getQueryDescriptor() {
        return queryDescriptor;
    }

    public String getQueryContent() {
        return queryContent;
    }

    @Override
    public String toString() {
        return this.queryDescriptor + " " + queryContent;
    }

}
