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

    //主窗体初始化
    MdEditorClient()
    {
        initTextPane();//面板
        initMenu();//菜单
        initAboutDialog();//关于对话框
        initToolBar();//工具栏
        
		setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
		setTitle("MdEditor");
		setSize(800,600);
		show();
		
		connect();
    }
    
    //面板初始化
    void initTextPane()
    {
    	setFont(new Font("Times New Roman",Font.PLAIN,12));
    	p.setLayout(new GridLayout(1,3,10,0));//10来控制布局管理器垂直间隔大小    	
        text.getDocument().addDocumentListener(new DocumentListener() {
            public void insertUpdate(DocumentEvent e) {
            	//freshPreview();
            	freshBar();
            	
            	if(freshstart==1){
            		freshstart=0;
            		freshPreview();
            	}
            	
            	if(CSCWflag==0){ //如果是自己更新了
                	freshPreview();
                	informServer(0);
                }
            	else if(CSCWflag==1){ //如果是被自己set了
                	CSCWflag=2;
                }
                else if(CSCWflag==2){ //如果是被别人set了
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

    //一级菜单设置
    JMenu [] menus= new JMenu[]
    {
        new JMenu("文件"),
        new JMenu("CSS设置"),
        new JMenu("帮助"),
    };

    //二级菜单设置
    JMenuItem menuitems [][] =new JMenuItem[][]
    {
        {
            new JMenuItem("新建文件"),
            new JMenuItem("打开文件"),
            new JMenuItem("保存文件"),
            new JMenuItem("导出为word"),
            new JMenuItem("退出")
        },
        {
            new JMenuItem("导入自定义CSS")
        },
        {
            new JMenuItem("说明")
        }
    };
    
    //菜单初始化
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

    //监听事件
    ActionListener action = new ActionListener()
    {
        public void actionPerformed(ActionEvent e)
        {
            JMenuItem mi = (JMenuItem)e.getSource();

            String id = mi.getText();
            if(id.equals("新建文件")){
            	Date date=new Date();
                text.setText(date.toString());
                
                file = null;
            }
            else if(id.equals("打开文件")){
                if(file != null)
                	filechooser.setSelectedFile(file);

                int returnVal = filechooser.showOpenDialog(MdEditorClient.this);

                if(returnVal == JFileChooser.APPROVE_OPTION){
                    file = filechooser.getSelectedFile();
                    openFile();
                    //informServer(0);
                }
            }
            else if(id.equals("保存文件")){
                if(file != null)
                	filechooser.setSelectedFile(file);

                int returnVal = filechooser.showSaveDialog(MdEditorClient.this);

                if(returnVal == JFileChooser.APPROVE_OPTION){
                    file = filechooser.getSelectedFile();
                    saveFile();
                }
            }
            else if(id.equals("导出为word")){ 
            	filechooser.setSelectedFile(new File(".docx")); //用了用户体验，设置一下默认后缀
                int returnVal = filechooser.showSaveDialog(MdEditorClient.this);

                if(returnVal == JFileChooser.APPROVE_OPTION){
                    file = filechooser.getSelectedFile();
                    saveFile2Word();
                } 
            }
            else if(id.equals("退出")){
            	System.exit(0);
            }
            else if(id.equals("导入自定义CSS")){
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
            
            else if(id.equals("说明")){
            	about.setSize(250,150);
            	about.show();
            }
            	
            }
        };

    //保存文件
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
    	//这一段的目的是为了先建立docx文件，因为后面wordMLPackage.save()好像需要一个已经存在的文件才行，虽然这样很不优雅
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
			String sss = "<html><head><meta http-equiv=\"content-type\" content=\"text/html; charset=gbk\" /><title>Markdown预览</title><link href=\""+css+"\" rel=\"stylesheet\" type=\"text/css\"/></head>";
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

    //打开文件
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

    //说明
    void initAboutDialog()
    {
        about.setTitle("说明");
        about.getContentPane().setBackground(Color.white );
        about.getContentPane().add(new JLabel("其实也没有什么好说了的"));
        about.setModal(true);

    }

    //工具栏设置
    JToolBar toolbar = new JToolBar();
    JButton [] buttons = new JButton[]
    {
        new JButton("",new ImageIcon("src/images/copy.png")),
        new JButton("",new ImageIcon("src/images/cut.png")),
        new JButton("",new ImageIcon("src/images/paste.png")),       
        new JButton("",new ImageIcon("src/images/view.png")),
    };

    //工具栏初始化及监听
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
        		text.setText("哎呀呀");
        		//freshPreview(); //更新预览
        		//freshBar(); //更新导航栏
        		//informServer(0);
        	}
        });

        this.getContentPane().add(toolbar,BorderLayout.NORTH);
	}

    //更新预览
    public void freshPreview() {
    	System.out.println("freshPreview被调用");
		preview_count=1-preview_count;
		System.out.println(preview_count);
    	//将当前页面内容生成html
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
			String sss = "<html><head><meta http-equiv=\"content-type\" content=\"text/html; charset=gbk\" /><title>Markdown预览</title><link href=\""+css+"\" rel=\"stylesheet\" type=\"text/css\"/></head>";
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
    
    //更新导航栏
    public void freshBar() {
    	DefaultMutableTreeNode root = new DefaultMutableTreeNode("导航");// 根节点进行初始化
        JTree jbar=new JTree(root);
        jbar.addTreeSelectionListener(new javax.swing.event.TreeSelectionListener() {
            public void valueChanged(javax.swing.event.TreeSelectionEvent evt) {
            	//setCaretPosition实现的跳转
            	DefaultMutableTreeNode selectedNode=(DefaultMutableTreeNode) jbar.getLastSelectedPathComponent();//返回最后选定的节点
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
        DefaultTreeModel model = (DefaultTreeModel) jbar.getModel();// 获得数据对象DefaultTreeModel
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
				//jbar的字符串
				String tmp2=line.substring(line.indexOf("<h1>")+4, line.indexOf("</h1>"));
                child = new DefaultMutableTreeNode(tmp2);// 生成子节点
                lasth1=(DefaultMutableTreeNode) child;
                model.insertNodeInto((MutableTreeNode) child, root, root.getChildCount());// 把child添加到chosen
			}
			else if(line.contains("<h2>")){
				//jbar的字符串
				String tmp2=line.substring(line.indexOf("<h2>")+4, line.indexOf("</h2>"));
                child = new DefaultMutableTreeNode(tmp2);// 生成子节点
                lasth2=(DefaultMutableTreeNode) child;     
                model.insertNodeInto((MutableTreeNode) child, lasth1, lasth1.getChildCount());// 把child添加到chosen
			}
			else if(line.contains("<h3>")){
				//jbar的字符串
				String tmp2=line.substring(line.indexOf("<h3>")+4, line.indexOf("</h3>"));
                child = new DefaultMutableTreeNode(tmp2);// 生成子节点
                lasth3=(DefaultMutableTreeNode) child;
                model.insertNodeInto((MutableTreeNode) child, lasth2, lasth2.getChildCount());// 把child添加到chosen
			}
			else if(line.contains("<h4>")){
				//jbar的字符串
				String tmp2=line.substring(line.indexOf("<h4>")+4, line.indexOf("</h4>"));
				child = new DefaultMutableTreeNode(tmp2);// 生成子节点
                lasth4=(DefaultMutableTreeNode) child;
                model.insertNodeInto((MutableTreeNode) child, lasth3, lasth3.getChildCount());// 把child添加到chosen
			}
			else if(line.contains("<h5>")){
				//jbar的字符串
				String tmp2=line.substring(line.indexOf("<h5>")+4, line.indexOf("</h5>"));
				child = new DefaultMutableTreeNode(tmp2);// 生成子节点
                lasth5=(DefaultMutableTreeNode) child;
                model.insertNodeInto((MutableTreeNode) child, lasth4, lasth4.getChildCount());// 把child添加到chosen
			}
			else if(line.contains("<h6>")){
				//jbar的字符串
				String tmp2=line.substring(line.indexOf("<h6>")+4, line.indexOf("</h6>"));
				child = new DefaultMutableTreeNode(tmp2);// 生成子节点
                model.insertNodeInto((MutableTreeNode) child, lasth5, lasth5.getChildCount());// 把child添加到chosen
			}
		}
		ecTreeTest(jbar); //设置JTree默认状态为展开
		bar.getViewport().add(jbar);
    }
    
    //展开JTree
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
    
    //通知服务器
    void informServer(int n){
    	if(n==0){
			try{
				String content = text.getText();       			
				//System.out.println("准备发给服务器的：");
				//System.out.println(content);
				toServer.writeUTF(content);
				//System.out.println(new Date());
				//System.out.println("通知完服务器了");
			} catch (IOException e2) {
				// TODO Auto-generated catch block
				e2.printStackTrace();
			}
    	}
    }
    
    //通知服务器Css的改变
    void informServerCss(String css){
    	try{      			
			//System.out.println("准备发给服务器的Css：");
			//System.out.println(css);
			toServer.writeUTF("css"+css);
			//System.out.println(new Date());
			//System.out.println("通知完服务器Css了");
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
        
        //得到服务器传来的字符串
        public void run(){
        	try {
        		while(true){
        			String content=fromServer.readUTF();
        			//CSCWRecieveflag=1;
        			//CSCWflag=1;
        			if(!content.startsWith("css")){
	        			/*
        				System.out.println(new Date());
	        			System.out.println("从服务器接收的：");
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
        			else{//更新css
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
    
	//程序入口函数
	public static void main( String [] args){
		MdEditorClient f = new MdEditorClient();
		f.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
		f.setTitle("MdEditor");
		f.setSize(800,600);
		f.show();
	}
    
}

