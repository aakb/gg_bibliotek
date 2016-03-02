package dk.aakb.itk.gg_bibliotek;

public class UndeliveredFile {
    private String filePath;
    private String eventUrl;
    private String fileDeliveryUrl;

    public UndeliveredFile(String filePath, String eventUrl, String fileDeliveryUrl) {
        this.filePath = filePath;
        this.eventUrl = eventUrl;
        this.fileDeliveryUrl = fileDeliveryUrl;
    }

    public String getEventUrl() {
        return eventUrl;
    }

    public void setEventUrl(String eventUrl) {
        this.eventUrl = eventUrl;
    }

    public String getFileDeliveryUrl() {
        return fileDeliveryUrl;
    }

    public void setFileDeliveryUrl(String fileDeliveryUrl) {
        this.fileDeliveryUrl = fileDeliveryUrl;
    }

    public String getFilePath() {
        return filePath;
    }

    public void setFilePath(String filePath) {
        this.filePath = filePath;
    }
}
