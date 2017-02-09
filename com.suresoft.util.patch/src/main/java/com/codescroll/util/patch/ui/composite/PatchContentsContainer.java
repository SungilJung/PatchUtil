package com.codescroll.util.patch.ui.composite;

import org.eclipse.swt.SWT;
import org.eclipse.swt.events.SelectionAdapter;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Display;
import org.eclipse.swt.widgets.Label;
import org.eclipse.swt.widgets.Text;

import com.codescroll.util.patch.Const;
import com.codescroll.util.patch.internal.Messages;

/**
 * 패치 내용 부분
 * <p>
 * 재사용 가능하도록 Extract Class 함<br>
 * parent Composite의 기본 numColumn은 3이여야 한다.<br>
 * 두 개의 버튼을 각각 용도에 맞게 설정하여 사용.
 * 
 * @author kbj
 */
public class PatchContentsContainer {
	
	private int BUTTON_MAX_SIZE = 2;
	
	private Composite container;
	private Text infoText;
	private Button[] buttons = new Button[BUTTON_MAX_SIZE];
	private String[] buttonNames = new String[BUTTON_MAX_SIZE];
	
	public static final int HEIGHT_HINT = 300; 
	public static final int WIDTH_HINT = 400;

	public PatchContentsContainer(Composite parent, String[] buttonNames) {

		this.container = parent;
		
		if(buttonNames.length > BUTTON_MAX_SIZE){
			throw new ArrayIndexOutOfBoundsException(Messages.ArrayIndexOutOfBoundsException);
		}
		this.buttonNames = buttonNames;
	}
	
	public void createPartControl() {
		createPartControl(HEIGHT_HINT, WIDTH_HINT);
	}
	
	public void createPartControl(int widthHint, int heightHint) {

		Label patchInfoLabel = new Label(container, SWT.NONE);
		patchInfoLabel.setText(Messages.PatchContentsComposite);
		GridData gd = new GridData(GridData.FILL, GridData.FILL, true, false, Const.DEFAULT_COMPOSITE_COL_NUM, 1);
		patchInfoLabel.setLayoutData(gd);

		infoText = new Text(container, SWT.MULTI | SWT.V_SCROLL | SWT.BORDER | SWT.WRAP);
		gd = new GridData(GridData.FILL, GridData.FILL, true, true, Const.DEFAULT_COMPOSITE_COL_NUM, 1);
		gd.heightHint = heightHint;
		gd.widthHint = widthHint;
		infoText.setLayoutData(gd);
		infoText.setEditable(false);
		infoText.setBackground(Display.getCurrent().getSystemColor(SWT.COLOR_WHITE));

		createButtonBar();
	}

	/**
	 * 전달받은 버튼의 컨텐츠로 하단 버튼 2개를 생성한다.
	 * 
	 */
	private void createButtonBar() {

		int index = 0;
		for(String buttonName : buttonNames){
			Button button = new Button(container, SWT.PUSH);
			GridData gdData = new GridData();
			gdData.widthHint = 100;
			button.setLayoutData(gdData);
			
			if(index == 0){
				// parent Composite의 기본 numColumn은 3이기 때문
				gdData.horizontalSpan = 2;
			}else{
				gdData.horizontalAlignment = SWT.END; 
			}
			button.setText(buttonName);
			buttons[index] = button;
			index++;
			
		}
		
	}
	
	/**
	 * 버튼의 enable 여부를 변경한다.
	 * @param btName 버튼의 getText()의 반환값과 일치하는 버튼명
	 * @param enabled
	 */
	public void setEnabledButton(String btName, boolean enabled){
		for(Button button : buttons){
			if(button.getText().equals(btName)){
				button.setEnabled(enabled);
			}
		}
	}
	
	/**
	 * 해당 버튼명의 버튼의 focus를 설정한다.
	 * @param btName btName 버튼의 getText()의 반환값과 일치하는 버튼명
	 */
	public void setFocusOfButton(String btName){
		for(Button button: buttons){
			if(button.getText().equals(btName)){
				button.setFocus();
			}
		}
	}
	
	/**
 	 * 버튼에 selectionAdapter를 등록한다.
	 * @param btName 버튼의 getText()의 반환값과 일치하는 버튼명
	 * @param selectionAdpater
	 */
	public void registerButtonSelectionAdpater(String btName, SelectionAdapter selectionAdpater){
		for(Button button : buttons){
			if(button.getText().equals(btName)){
				button.addSelectionListener(selectionAdpater);
			}
		}
	}
	

	/**
	 * 패치 관련 텍스트를 Text에 설정하는 메서드
	 * @param contents 보이도록 설정할 파일 경로
	 */
	public void setViewerContents(String contents) {
		System.out.println("call setViewerContents()");
		infoText.setText(contents);
	}
	
	
	/**
	 * 패치 내용 Text의 Enable 여부를 설정하는 메서드
	 * 
	 * @param enabled Enable 여부
	 */
	public void setEnabledText(boolean enabled) {
		infoText.setEnabled(enabled);
		
		if(enabled) {
			infoText.setBackground(Display.getCurrent().getSystemColor(SWT.COLOR_WHITE));
			
		} else {
			infoText.setBackground(Display.getCurrent().getSystemColor(SWT.COLOR_WIDGET_BACKGROUND));
		}
	}

}
