import java.io.*;
import java.net.*;
import java.util.*;


public class MdEditorServer {  
    List<ClientThread> clients = new ArrayList<ClientThread>();  
    
    public void start(){
        try {
            boolean iConnect = false;
            ServerSocket ss = new ServerSocket(1720);
            iConnect = true;
            while(iConnect){
            	System.out.println("�󶨷������˿ڳɹ���");
                Socket s = ss.accept();
                ClientThread currentClient = new ClientThread(s);
                DataOutputStream dos = new DataOutputStream(s.getOutputStream());
                //dos.writeInt(ID++);
                clients.add(currentClient);
                new Thread(currentClient).start();
                System.out.println("�ͻ��˽����Ѿ�������");
            }
        } catch (IOException e) {
            System.out.println("IOException");
            e.printStackTrace();
        }
    }
    
    class ClientThread implements Runnable {
        private Socket s;
        private DataInputStream dis;
        private DataOutputStream dos;
        private String str;
        private boolean iConnect = false;

        ClientThread(Socket s){
            this.s = s;
            iConnect = true;
            try {
				dis = new DataInputStream(s.getInputStream());
				dos = new DataOutputStream(s.getOutputStream());
			} catch (IOException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
        }
        
        public void run(){
            try {
                
                while(iConnect){
                	System.out.println("���ڵȴ��ͻ��˵ķ�����Ϣ...");
                    //dis = new DataInputStream(s.getInputStream());
                    str = dis.readUTF();
                    //System.out.println(str);
                    for(int i=0; i<clients.size(); i++){
                        ClientThread c = clients.get(i);
                        if(c!=this){
                        	System.out.println(new Date());
                        	System.out.println("ת����Ϣ��..."+i);
                        	c.sendMsg(str);
                        }
                    }
                    /*
                    try {
						Thread.sleep(100);
					} catch (InterruptedException e) {
						// TODO Auto-generated catch block
						e.printStackTrace();
					}
					*/
                }
            } catch (IOException e) {
                e.printStackTrace();
            }
            
        }
        
        public void sendMsg(String str){
            try {
                //dos = new DataOutputStream(this.s.getOutputStream());
                System.out.println("������ͻ���д��Ϣ��");
                dos.writeUTF(str);
                System.out.println("������ͻ���д��Ϣ�ɹ���");            
            } catch (IOException e) {
                e.printStackTrace();
            }
            
        }
        
    }
    

    public static void main(String[] args) {
        new MdEditorServer().start();
    }
    
}