import java.io.IOException;
import java.net.InetSocketAddress;
import java.nio.ByteBuffer;
import java.nio.channels.ServerSocketChannel;
import java.nio.channels.SocketChannel;
import java.nio.charset.Charset;
import java.util.ArrayList;
import java.util.List;


public class ServeurChatMonoThread {
    private boolean isActive = true;
    private int nombreClients = 0;
    private List<Conversation> clients = new ArrayList<>();

    public static void main(String[] args) {
        new ServeurChat().start();
    }

    public void start() {
        try {
            ServerSocketChannel serverSocketChannel = ServerSocketChannel.open();
            serverSocketChannel.bind(new InetSocketAddress(1234));

            while (isActive) {
                SocketChannel socketChannel = serverSocketChannel.accept();
                ++nombreClients;
                Conversation conversation = new Conversation(socketChannel, nombreClients);
                clients.add(conversation);
                conversation.start();
            }

        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    class Conversation extends Thread {
        protected SocketChannel socketChannel;
        protected int numero;

        public Conversation(SocketChannel socketChannel, int numero) {
            this.socketChannel = socketChannel;
            this.numero = numero;
        }

        public void broadcastMessage(String message, SocketChannel senderChannel, int numClient) {
            try {
                for (Conversation client : clients) {
                    if (client.socketChannel != senderChannel) {
                        if (client.numero == numClient || numClient == -1) {
                            ByteBuffer buffer = ByteBuffer.wrap(message.getBytes());
                            client.socketChannel.write(buffer);
                        }
                    }
                }
            } catch (IOException e) {
                e.printStackTrace();
            }
        }

        @Override
        public void run() {
            try {
                ByteBuffer buffer = ByteBuffer.allocate(1024);
                Charset charset = Charset.forName("UTF-8");

                socketChannel.write(charset.encode("Bienvenue, vous etes le client numero: " + numero));
                String ipClient = socketChannel.getRemoteAddress().toString();
                System.out.println("Connexion du client numero: " + numero + ", IP: " + ipClient);

                while (true) {
                    buffer.clear();
                    int bytesRead = socketChannel.read(buffer);
                    if (bytesRead == -1) {
                        break;
                    }

                    buffer.flip();
                    String req = charset.decode(buffer).toString().trim();

                    if (req.contains("=>")) {
                        String[] requestParams = req.split("=>");
                        if (requestParams.length == 2) {
                            String message = requestParams[1];
                            try {
                                int numeroClient = Integer.parseInt(requestParams[0]);
                                broadcastMessage(message, socketChannel, numeroClient);
                            } catch (NumberFormatException e) {
                                e.printStackTrace();
                            }
                        }
                    } else {
                        broadcastMessage(req, socketChannel, -1);
                    }
                }

                socketChannel.close();
                System.out.println("Déconnexion du client numéro: " + numero);

            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }
}
