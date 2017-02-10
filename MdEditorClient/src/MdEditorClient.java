import java.awt.*;
import java.awt.event.*;
import javax.swing.*;
import javax.swing.event.DocumentEvent;
import javax.swing.event.DocumentListener;
import javax.swing.text.html.HTMLDocument;
import javax.swing.text.html.HTMLEditorKit;
import javax.swing.tree.*;

import java.io.*;
import java.net.*;
import java.util.*;

import com.petebevin.markdown.MarkdownProcessor;
import org.docx4j.convert.in.xhtml.XHTMLImporterImpl;
import org.docx4j.model.structure.PageSizePaper;
import org.docx4j.openpackaging.exceptions.Docx4JException;
import org.docx4j.openpackaging.exceptions.InvalidFormatException;
import org.docx4j.openpackaging.packages.WordprocessingMLPackage;
import org.jsoup.Jsoup;
import org.markdown4j.Markdown4jProcessor;
class MdEditorClient extends JFrame
{
    File file = null;
    Boolean cssflag = new Boolean("false"); 
    Color color = Color.black;
    GraphicsEnvironment getFont = GraphicsEnvironment.getLocalGraphicsEnvironment();
    Font []fonts = getFont.getAllFonts();
    Integer count=0;
    Integer preview_count=0;
    JPanel p=new JPanel();
    JScrollPane bar=new JScrollPane();
    JTextPane text = new JTextPane();
    JTextPane view = new JTextPane();
    JFileChooser filechooser = new JFileChooser();
    JColorChooser colorchooser = new JColorChooser();
    JDialog about = new JDialog(this);
    JMenuBar menubar = new JMenuBar(); 
    String host="localhost";
    Socket socket;
    DataOutputStream toServer;
    DataInputStream fromServer;
    //Integer CSCWSendflag=0;
    Integer CSCWRecieveflag=0;
    Integer CSCWflag=0;
    Integer freshstart=1;

    //�������ʼ��
    MdEditorClient()
    {
        initTextPane();//���
        initMenu();//�˵�
        initAboutDialog();//���ڶԻ���
        initToolBar();//������
        
		setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
		setTitle("MdEditor");
		setSize(800,600);
		show();
		
		connect();
    }
    
    //����ʼ��
    void initTextPane()
    {
    	setFont(new Font("Times New Roman",Font.PLAIN,12));
    	p.setLayout(new GridLayout(1,3,10,0));//10�����Ʋ��ֹ�������ֱ�����С    	
        text.getDocument().addDocumentListener(new DocumentListener() {
            public void insertUpdate(DocumentEvent e) {
            	//freshPreview();
            	freshBar();
            	
            	if(freshstart==1){
            		freshstart=0;
            		freshPreview();
            	}
            	
            	if(CSCWflag==0){ //������Լ�������
                	freshPreview();
                	informServer(0);
                }
            	else if(CSCWflag==1){ //����Ǳ��Լ�set��
                	CSCWflag=2;
                }
                else if(CSCWflag==2){ //����Ǳ�����set��
                	freshPreview();
                	CSCWflag=0;
                }

            }

            public void removeUpdate(DocumentEvent e) {
            	//freshPreview();
            	freshBar();
            	
            	if(freshstart==1){
            		freshstart=0;
            		freshPreview();
            	}
            	
            	if(CSCWflag==0){
                	freshPreview();
                	informServer(0);
                }
            	else if(CSCWflag==1){
                	CSCWflag=2;
                }
                else if(CSCWflag==2){
                	freshPreview();
                	CSCWflag=0;
                }
            }

            public void changedUpdate(DocumentEvent e) {
            	//freshPreview();
            	freshBar();
            	
            	if(freshstart==1){
            		freshstart=0;
            		freshPreview();
            	}
            	
            	if(CSCWflag==0){
                	freshPreview();
                	informServer(0);
                }
            	else if(CSCWflag==1){
                	CSCWflag=2;
                }
                else if(CSCWflag==2){
                	freshPreview();
                	CSCWflag=0;
                }
            }
        });
    	view.setEditable(false);
    	p.add(bar);
    	p.add(new JScrollPane(text));
    	p.add(new JScrollPane(view));
    	getContentPane().add(p);
    }

    //һ���˵�����
    JMenu [] menus= new JMenu[]
    {
        new JMenu("�ļ�"),
        new JMenu("CSS����"),
        new JMenu("����"),
    };

    //�����˵�����
    JMenuItem menuitems [][] =new JMenuItem[][]
    {
        {
            new JMenuItem("�½��ļ�"),
            new JMenuItem("���ļ�"),
            new JMenuItem("�����ļ�"),
            new JMenuItem("����Ϊword"),
            new JMenuItem("�˳�")
        },
        {
            new JMenuItem("�����Զ���CSS")
        },
        {
            new JMenuItem("˵��")
        }
    };
    
    //�˵���ʼ��
    void initMenu()
    {
        for(int i=0;i<menus.length;i++)
        {
            menubar.add(menus[i]);
            for(int j=0;j<menuitems[i].length;j++)
            {
                menus[i].add(menuitems[i][j]);

                menuitems[i][j].addActionListener(action);
            }
        }

        this.setJMenuBar(menubar);
    }

    //�����¼�
    ActionListener action = new ActionListener()
    {
        public void actionPerformed(ActionEvent e)
        {
            JMenuItem mi = (JMenuItem)e.getSource();

            String id = mi.getText();
            if(id.equals("�½��ļ�")){
            	Date date=new Date();
                text.setText(date.toString());
                
                file = null;
            }
            else if(id.equals("���ļ�")){
                if(file != null)
                	filechooser.setSelectedFile(file);

                int returnVal = filechooser.showOpenDialog(MdEditorClient.this);

                if(returnVal == JFileChooser.APPROVE_OPTION){
                    file = filechooser.getSelectedFile();
                    openFile();
                    //informServer(0);
                }
            }
            else if(id.equals("�����ļ�")){
                if(file != null)
                	filechooser.setSelectedFile(file);

                int returnVal = filechooser.showSaveDialog(MdEditorClient.this);

                if(returnVal == JFileChooser.APPROVE_OPTION){
                    file = filechooser.getSelectedFile();
                    saveFile();
                }
            }
            else if(id.equals("����Ϊword")){ 
            	filechooser.setSelectedFile(new File(".docx")); //�����û����飬����һ��Ĭ�Ϻ�׺
                int returnVal = filechooser.showSaveDialog(MdEditorClient.this);

                if(returnVal == JFileChooser.APPROVE_OPTION){
                    file = filechooser.getSelectedFile();
                    saveFile2Word();
                } 
            }
            else if(id.equals("�˳�")){
            	System.exit(0);
            }
            else if(id.equals("�����Զ���CSS")){
                int returnVal = filechooser.showOpenDialog(MdEditorClient.this);

                if(returnVal == JFileChooser.APPROVE_OPTION){
                    file = filechooser.getSelectedFile();
                    try
                    {
                        BufferedReader fr = new BufferedReader(new FileReader(file.getAbsolutePath()));
                        try
                        {
                            FileWriter fw = new FileWriter("src/css/my.css");
                            String str;
                            String css = null;
                            while((str=fr.readLine())!=null){
                                fw.write(str);
                                css=css+str;
                            }
                            fw.close();
                            fr.close();
                            cssflag=true;
                            freshPreview();
                            informServerCss(css);
                        } catch (IOException e1)
                        {
                            // TODO Auto-generated catch block
                            e1.printStackTrace();
                        }
                    } catch (FileNotFoundException e1)
                    {
                        // TODO Auto-generated catch block
                        e1.printStackTrace();
                    }
                }             
            }
            
            else if(id.equals("˵��")){
            	about.setSize(250,150);
            	about.show();
            }
            	
            }
        };

    //�����ļ�
    void saveFile()
    {
        try{
            FileWriter fw = new FileWriter(file);
            fw.write(text.getText());
            fw.close();
        }
        catch(Exception e)
        {
            e.printStackTrace();
        }
    }
    
    void saveFile2Word()
    {
    	//��һ�ε�Ŀ����Ϊ���Ƚ���docx�ļ�����Ϊ����wordMLPackage.save()������Ҫһ���Ѿ����ڵ��ļ����У���Ȼ�����ܲ�����
        try{
            FileWriter fw = new FileWriter(file);
            fw.close();
        }
        catch(Exception e)
        {
            e.printStackTrace();
        }
    	WordprocessingMLPackage wordMLPackage;
    	String html=null;
        try {
			String content = text.getText();       			
			html = new MarkdownProcessor().markdown(content);
			String css = null;
			if(!cssflag){
				css = "src/css/default.css";
			}
			else{
				css = "src/css/my.css";
			}
			String sss = "<html><head><meta http-equiv=\"content-type\" content=\"text/html; charset=gbk\" /><title>MarkdownԤ��</title><link href=\""+css+"\" rel=\"stylesheet\" type=\"text/css\"/></head>";
			String abc = sss+html+"</html>";
        	wordMLPackage = WordprocessingMLPackage.createPackage(PageSizePaper.A4, true);
        	XHTMLImporterImpl xhtmlImporter = new XHTMLImporterImpl(wordMLPackage);
            wordMLPackage.getMainDocumentPart().getContent().addAll(xhtmlImporter.convert(Jsoup.parse(html).html(), null));
			wordMLPackage.save(file);
		} catch (Exception e1) {
			// TODO Auto-generated catch block
			e1.printStackTrace();
		}
    }

    //���ļ�
    void openFile()
    {
        try{
            FileReader fr = new FileReader(file);
            int len = (int) file.length();
            char [] buffer = new char[len];
            fr.read(buffer,0,len);
            fr.close();
            text.setText(new String(buffer));
        }
        catch(Exception e)
        {
            e.printStackTrace();
        }
    }

    //˵��
    void initAboutDialog()
    {
        about.setTitle("˵��");
        about.getContentPane().setBackground(Color.white );
        about.getContentPane().add(new JLabel("��ʵҲû��ʲô��˵�˵�"));
        about.setModal(true);

    }

    //����������
    JToolBar toolbar = new JToolBar();
    JButton [] buttons = new JButton[]
    {
        new JButton("",new ImageIcon("src/images/copy.png")),
        new JButton("",new ImageIcon("src/images/cut.png")),
        new JButton("",new ImageIcon("src/images/paste.png")),       
        new JButton("",new ImageIcon("src/images/view.png")),
    };

    //��������ʼ��������
    void initToolBar()
    {
        for(int i=0; i<buttons.length;i++)
        toolbar.add(buttons[i]);
        buttons[0].setToolTipText("copy");
        buttons[0].addActionListener(new ActionListener(){public void actionPerformed(ActionEvent e){text.copy();}});

        buttons[1].setToolTipText("cut");
        buttons[1].addActionListener(new ActionListener(){public void actionPerformed(ActionEvent e){text.cut();}});

        buttons[2].setToolTipText("paste");
        buttons[2].addActionListener(new ActionListener(){public void actionPerformed(ActionEvent e){text.paste();}});
        
        buttons[3].setToolTipText("view");
        buttons[3].addActionListener(new ActionListener(){
        	public void actionPerformed(ActionEvent e){
        		text.setText("��ѽѽ");
        		//freshPreview(); //����Ԥ��
        		//freshBar(); //���µ�����
        		//informServer(0);
        	}
        });

        this.getContentPane().add(toolbar,BorderLayout.NORTH);
	}

    //����Ԥ��
    public void freshPreview() {
    	System.out.println("freshPreview������");
		preview_count=1-preview_count;
		System.out.println(preview_count);
    	//����ǰҳ����������html
		String html = null;
		try{
			String content = text.getText();       			
			html = new Markdown4jProcessor().process(content);
			FileWriter htmlWriter;
			htmlWriter = new FileWriter("preview"+preview_count+".html");
			String css = null;
			if(!cssflag){
				css = "src/css/default.css";
			}
			else{
				css = "src/css/my.css";
			}
			String sss = "<html><head><meta http-equiv=\"content-type\" content=\"text/html; charset=gbk\" /><title>MarkdownԤ��</title><link href=\""+css+"\" rel=\"stylesheet\" type=\"text/css\"/></head>";
			String abc = sss+html+"</html>";
			htmlWriter.write(abc);
			htmlWriter.close();
			//System.out.println("Client refresh self");
			String path1=new String("file:///"+System.getProperty("user.dir")+"/preview"+preview_count+".html");
			view.setPage(path1);
		} catch (IOException e2) {
			// TODO Auto-generated catch block
			e2.printStackTrace();
		}    		
    }
    
    //���µ�����
    public void freshBar() {
    	DefaultMutableTreeNode root = new DefaultMutableTreeNode("����");// ���ڵ���г�ʼ��
        JTree jbar=new JTree(root);
        jbar.addTreeSelectionListener(new javax.swing.event.TreeSelectionListener() {
            public void valueChanged(javax.swing.event.TreeSelectionEvent evt) {
            	//setCaretPositionʵ�ֵ���ת
            	DefaultMutableTreeNode selectedNode=(DefaultMutableTreeNode) jbar.getLastSelectedPathComponent();//�������ѡ���Ľڵ�
            	int a = 0, b = 0;
    			int FindStartPos = 0;
    			String strA = text.getText();
    			String strB = selectedNode.toString();
    			System.out.println(strB);
    			a = strA.indexOf(strB,FindStartPos);
    			if(a > -1) {
    				text.setCaretPosition(a);
    				b = strB.length();
    				text.select(a, a + b);
    			}
        }});
        DefaultTreeModel model = (DefaultTreeModel) jbar.getModel();// ������ݶ���DefaultTreeModel
		String content = text.getText();       			
		String html = null;
		try {
			html = new Markdown4jProcessor().process(content);
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		String[] lines=html.split("\n");
		LinkedList<String> barlines = new LinkedList<String>(); 
		DefaultMutableTreeNode lasth1 = null,lasth2 = null,lasth3 = null,lasth4 = null,lasth5 = null;
		Object child;
		for(String line:lines) {
			if(line.contains("<h1>")){
				//jbar���ַ���
				String tmp2=line.substring(line.indexOf("<h1>")+4, line.indexOf("</h1>"));
                child = new DefaultMutableTreeNode(tmp2);// �����ӽڵ�
                lasth1=(DefaultMutableTreeNode) child;
                model.insertNodeInto((MutableTreeNode) child, root, root.getChildCount());// ��child��ӵ�chosen
			}
			else if(line.contains("<h2>")){
				//jbar���ַ���
				String tmp2=line.substring(line.indexOf("<h2>")+4, line.indexOf("</h2>"));
                child = new DefaultMutableTreeNode(tmp2);// �����ӽڵ�
                lasth2=(DefaultMutableTreeNode) child;     
                model.insertNodeInto((MutableTreeNode) child, lasth1, lasth1.getChildCount());// ��child��ӵ�chosen
			}
			else if(line.contains("<h3>")){
				//jbar���ַ���
				String tmp2=line.substring(line.indexOf("<h3>")+4, line.indexOf("</h3>"));
                child = new DefaultMutableTreeNode(tmp2);// �����ӽڵ�
                lasth3=(DefaultMutableTreeNode) child;
                model.insertNodeInto((MutableTreeNode) child, lasth2, lasth2.getChildCount());// ��child��ӵ�chosen
			}
			else if(line.contains("<h4>")){
				//jbar���ַ���
				String tmp2=line.substring(line.indexOf("<h4>")+4, line.indexOf("</h4>"));
				child = new DefaultMutableTreeNode(tmp2);// �����ӽڵ�
                lasth4=(DefaultMutableTreeNode) child;
                model.insertNodeInto((MutableTreeNode) child, lasth3, lasth3.getChildCount());// ��child��ӵ�chosen
			}
			else if(line.contains("<h5>")){
				//jbar���ַ���
				String tmp2=line.substring(line.indexOf("<h5>")+4, line.indexOf("</h5>"));
				child = new DefaultMutableTreeNode(tmp2);// �����ӽڵ�
                lasth5=(DefaultMutableTreeNode) child;
                model.insertNodeInto((MutableTreeNode) child, lasth4, lasth4.getChildCount());// ��child��ӵ�chosen
			}
			else if(line.contains("<h6>")){
				//jbar���ַ���
				String tmp2=line.substring(line.indexOf("<h6>")+4, line.indexOf("</h6>"));
				child = new DefaultMutableTreeNode(tmp2);// �����ӽڵ�
                model.insertNodeInto((MutableTreeNode) child, lasth5, lasth5.getChildCount());// ��child��ӵ�chosen
			}
		}
		ecTreeTest(jbar); //����JTreeĬ��״̬Ϊչ��
		bar.getViewport().add(jbar);
    }
    
    //չ��JTree
    public void ecTreeTest(JTree tree) {
        TreeNode root = (TreeNode) tree.getModel().getRoot();
        expandTree(tree, new TreePath(root));
    }
     
    private void expandTree(JTree tree, TreePath parent) {
        TreeNode node = (TreeNode) parent.getLastPathComponent();
        if (node.getChildCount() >= 0) {
           for (Enumeration<?> e = node.children(); e.hasMoreElements();) {
               TreeNode n = (TreeNode) e.nextElement();
               TreePath path = parent.pathByAddingChild(n);
               expandTree(tree, path);
           }
        }
        tree.expandPath(parent);
    }
    
    //֪ͨ������
    void informServer(int n){
    	if(n==0){
			try{
				String content = text.getText();       			
				//System.out.println("׼�������������ģ�");
				//System.out.println(content);
				toServer.writeUTF(content);
				//System.out.println(new Date());
				//System.out.println("֪ͨ���������");
			} catch (IOException e2) {
				// TODO Auto-generated catch block
				e2.printStackTrace();
			}
    	}
    }
    
    //֪ͨ������Css�ĸı�
    void informServerCss(String css){
    	try{      			
			//System.out.println("׼��������������Css��");
			//System.out.println(css);
			toServer.writeUTF("css"+css);
			//System.out.println(new Date());
			//System.out.println("֪ͨ�������Css��");
		} catch (IOException e2) {
			// TODO Auto-generated catch block
			e2.printStackTrace();
		}	
    }
    
    public void connect(){
		try {
			socket = new Socket(host,1720);
			fromServer=new DataInputStream(socket.getInputStream());
			toServer=new DataOutputStream(socket.getOutputStream());
			new Thread(new SendThread()).start();
		} catch (UnknownHostException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		
    }
    
    
    class SendThread implements Runnable{
        private String str;
        private boolean iConnect = false;
        
        //�õ��������������ַ���
        public void run(){
        	try {
        		while(true){
        			String content=fromServer.readUTF();
        			//CSCWRecieveflag=1;
        			//CSCWflag=1;
        			if(!content.startsWith("css")){
	        			/*
        				System.out.println(new Date());
	        			System.out.println("�ӷ��������յģ�");
	        			System.out.println(content);
	        			*/
						SwingUtilities.invokeLater(new Runnable() {
							@Override
							public void run() {
								CSCWRecieveflag=2;
			        			CSCWflag=1;
								text.setText(content);
							}
						});
        			}
        			else{//����css
        				String css=content.substring(3);
                        FileWriter fw = new FileWriter("src/css/my.css");
                        fw.write(css);
                        fw.close();
                        cssflag=true;
                        freshPreview();
        			}
        		}
	        } catch (IOException e) {
	    		// TODO Auto-generated catch block
	        	e.printStackTrace();
	    	}  	
        }       
    }
    
	//������ں���
	public static void main( String [] args){
		MdEditorClient f = new MdEditorClient();
		f.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
		f.setTitle("MdEditor");
		f.setSize(800,600);
		f.show();
	}
    
}

