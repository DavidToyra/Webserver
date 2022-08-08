/*
    David Töyrä
    Mainserver.java
    2019-09-19
 */
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.InetSocketAddress;
import java.nio.ByteBuffer;
import java.nio.channels.SelectionKey;
import java.nio.channels.Selector;
import java.nio.channels.ServerSocketChannel;
import java.nio.channels.SocketChannel;
import java.nio.charset.Charset;
import java.util.Iterator;
import java.util.NoSuchElementException;
import java.util.Scanner;
import java.util.StringTokenizer;


public class MainServer
{

    private static final String UNSUPPORTED_METHOD = "Method not supported";
    private static final String FILE_NOT_FOUND = "File not found";

    /**
     * Constructor for the server. Creates a server socket and binds it to localhost and the provided
     * port. Selector listens for clients that attempt to connect and then parses their messages and
     * sends back either the files or proper error messages.
     * @param port number for which port to use.
     * @param directory directory where the server can find the files to use.
     * @throws IOException
     */
    public MainServer(String port, String directory) throws IOException
    {
        //Open selector and attach it to a channel
        Selector selector = Selector.open();
        ServerSocketChannel server = ServerSocketChannel.open();
        server.bind(new InetSocketAddress("127.0.0.1", Integer.parseInt(port)));
        server.configureBlocking(false);
        server.register(selector, SelectionKey.OP_ACCEPT);
        String fileRequested = null;
        String header;

        //Adds a separator to the server directory if it doesn't have one
        if(!directory.endsWith("/"))
        {
            directory = directory+"/";
        }

        //Booleans for determining what to write to the channel
        boolean onlyHeader = true;
        boolean validMethod = true;

        boolean running = true;

        //Server loop goes on forever
        while(running)
        {

            if(selector.select(5000) == 0)
            {
                System.out.println("Timeout after 5 seconds, retrying\n");
            }

            //Loop through each key
            Iterator<SelectionKey> keys = selector.selectedKeys().iterator();
            while(keys.hasNext())
            {
                SelectionKey key = keys.next();
                keys.remove();

                if(key.isAcceptable())//If channel is acceptable
                {
                    ServerSocketChannel server1 = (ServerSocketChannel)key.channel();
                    SocketChannel channel = server1.accept();
                    channel.configureBlocking(false);
                    channel.register(selector, SelectionKey.OP_READ, ByteBuffer.allocate(1024));
                    System.out.println("Client connected!");
                }
                //If channel is readable
                if (key.isReadable())
                {

                    String input;
                    Scanner scanner = new Scanner((SocketChannel) key.channel());
                    try{
                        input = scanner.nextLine();
                    }
                    catch (NoSuchElementException e){
                        System.out.println("No line detected; "+e.getMessage());
                        selector.selectedKeys().clear();
                        break;

                }

                    //Parse the request with a string tokenizer
                    StringTokenizer parse = new StringTokenizer(input);

                    //Get the HTTP method of the client
                    String method = parse.nextToken().toUpperCase();
                    if (!method.equals("GET") && !method.equals("HEAD")) {
                        validMethod = false;
                        key.interestOps(SelectionKey.OP_WRITE);
                    }
                    else
                    {
                        // we get file requested
                        fileRequested = parse.nextToken().toLowerCase();

                        // we support only GET and HEAD methods, we check if they are or else return 402
                        // GET or HEAD method
                        if (fileRequested.endsWith("/"))
                        {
                            fileRequested += "html.index";
                        }

                        if (method.equals("GET"))
                        {
                            onlyHeader = false;
                        }
                        key.interestOps(SelectionKey.OP_WRITE);
                    }
                }

                else if(key.isWritable())
                {
                    File f = null;
                    SocketChannel channel = (SocketChannel)key.channel();

                    //If invalid method
                    if(!validMethod)
                    {
                        writeToSocket(channel, UNSUPPORTED_METHOD);
                        validMethod = true;
                    }

                    else if(fileRequested==null)
                    {

                        writeToSocket(channel, FILE_NOT_FOUND);
                    }
                    else
                    {
                        //If file is given but does not exists
                        f = new File(directory+fileRequested);
                        if(!f.exists())
                        {
                            writeToSocket(channel, FILE_NOT_FOUND);
                        }
                        else
                        {
                            //Prepare file header
                            int fileLength = (int) f.length();
                            String content = getContentType(fileRequested);
                            header = "HTTP/1.0 200 OK \r\n"+"Content-type: " +
                                    content+"\r\n"+"Content-length: " + fileLength+"\r\n\r\n";
                            //Send HTTP header
                            ByteBuffer byteBuffer = Charset.forName("UTF-8").encode(header);
                            channel.write(byteBuffer);

                            //IF GET request then send file as well
                            if(!onlyHeader)
                            {
                                ByteBuffer buffer = ByteBuffer.allocate((int)f.length());

                                InputStream fileStream = new FileInputStream(f);
                                int b;

                                while((b = fileStream.read()) != -1) {
                                    buffer.put((byte)b);
                                }

                                buffer.flip();
                                channel.write(buffer);
                            }
                        }
                    }
                    onlyHeader = true;
                    channel.close();
                }
                selector.selectedKeys().clear();
            }
        }
        server.close();

    }


    /**
     * Writes the header and the content to the channel socket in case of a 404.
     * @param channel the client's socket channel.
     * @param headerMessage the specific header body.
     * @throws IOException if write fails.
     */
    private void writeToSocket(SocketChannel channel, String headerMessage) throws IOException {
        String header = "HTTP/1.0 404 Not Found \r\n"+"Content-type: text"+
                "\r\n"+"Content-length: " + headerMessage.length()+"\r\n\r\n";
        ByteBuffer buffer = Charset.forName("UTF-8").encode(header);
        channel.write(buffer);
        buffer = Charset.forName("UTF-8"). encode(headerMessage);
        channel.write(buffer);
    }

    /**
     * Checks the file endings and return the type of the file, either html, text or png.
     * @param fileRequested the requested file from the client.
     * @return HTTP style file type.
     */
    private String getContentType(String fileRequested)
    {
        if (fileRequested.endsWith(".htm")  ||  fileRequested.endsWith(".html"))
            return "text/html";
        else if(fileRequested.endsWith(".png"))
            return "image/png";
        else
            return "text/plain";
    }
}
