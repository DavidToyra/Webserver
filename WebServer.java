/*
    Webserver.java
    David Töyrä
    2019-09-19
 */
import java.io.File;
import java.io.IOException;

class WebServer {

    /**
     * Main function for the web server. Checks the input and runs the server.
     * @param args
     * @throws IOException
     */
    public static void main(String[] args) throws IOException
    {

        //Check arguments
        if(args.length != 2)
        {
            System.err.println("Wrong arguments. Usage: java WebServer [port] [directory]");
            return;
        }

        File startDirectory = new File(args[1]);
        if(!startDirectory.isDirectory())
        {
            System.err.println("Second argument is not a directory");
            return;
        }

        MainServer server = new MainServer(args[0], args[1]);
    }


}
