package com.ubicomp.ketdiary.dialog;

import java.io.File;
import java.sql.Date;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Calendar;
import java.util.Collections;
import java.util.List;
import java.util.Random;
import android.annotation.SuppressLint;
import android.app.Activity;
import android.content.ActivityNotFoundException;
import android.content.Context;
import android.content.Intent;
import android.content.res.Resources;
import android.graphics.Color;
import android.graphics.Typeface;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.speech.RecognizerIntent;
import android.support.v4.view.PagerAdapter;
import android.support.v4.view.ViewPager;
import android.support.v4.view.ViewPager.OnPageChangeListener;
import android.text.Html;
import android.text.InputType;
import android.util.DisplayMetrics;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.MotionEvent;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.View.OnTouchListener;
import android.view.ViewGroup;
import android.view.inputmethod.InputMethodManager;
import android.widget.AdapterView;
import android.widget.AdapterView.OnItemClickListener;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.ListAdapter;
import android.widget.ListView;
import android.widget.RelativeLayout;
import android.widget.SeekBar;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.Toast;

import com.ubicomp.ketdiary.App;
import com.ubicomp.ketdiary.MainActivity;
import com.ubicomp.ketdiary.data.structure.DropEditText;
import com.ubicomp.ketdiary2.R;

import CustomList.CustomList;

import com.ubicomp.ketdiary.data.db.DatabaseControl;
import com.ubicomp.ketdiary.data.file.QuestionFile;
import com.ubicomp.ketdiary.data.structure.AddScore;
import com.ubicomp.ketdiary.noUse.NoteCatagory3;
import com.ubicomp.ketdiary.system.PreferenceControl;
import com.ubicomp.ketdiary.system.check.NetworkCheck;
import com.ubicomp.ketdiary.system.check.TimeBlock;
import com.ubicomp.ketdiary.system.clicklog.ClickLog;
import com.ubicomp.ketdiary.system.clicklog.ClickLogId;
import com.ubicomp.ketdiary.ui.BarButtonGenerator;
import com.ubicomp.ketdiary.ui.CustomScrollView;
import com.ubicomp.ketdiary.ui.CustomToast;
import com.ubicomp.ketdiary.ui.CustomToastSmall;
import com.ubicomp.ketdiary.ui.Typefaces;
import com.ubicomp.ketdiary.main.fragment.DaybookFragment;


/**
 * Note after testing
 * @author Andy
 *
 */
@SuppressLint("ResourceAsColor")
public class AddNoteDialog2 implements ChooseItemCaller/*, View.OnTouchListener*/{
	
	private Activity activity;
	private AddNoteDialog2 addNoteDialog = this;
	private static final String TAG = "ADD_PAGE";
	public AddNoteDialogThinking notePage2 = null;
	private QuestionIdentityDialog questionBox = null;
	
	
	private TestQuestionCaller2 testQuestionCaller;
	private Context context;
	private Resources resource;
	private LayoutInflater inflater;
	private RelativeLayout boxLayout = null;
	private LinearLayout center_layout, title_layout, main_layout, bottom_layout, title, date_layout, timeslot_layout;
	
	private RelativeLayout mainLayout;
	private View view;
	
	private ViewPager vPager;
	private ImageView iv_try, iv_smile, iv_urge,
					  iv_cry, iv_not_good;
	private ImageView iv_conflict, iv_social, iv_playing;
	private ImageView iv_self_others_bar;
	private Spinner sp_date, sp_timeslot;// sp_item;
	private Button bt_confirm, bt_cancel;
	private SeekBar impactSeekBar;
	private TextView text_self, text_other, text_item, text_impact, text_description,
	     tv_knowdlege, tv_title, note_title, sp_content, date_txt, timeslot_txt, title_txt, typetext;
	
	private TextView edtext;
	private ListView listView;
	private ListView listView2;
	
	private String[] coping_msg;
	private String[] knowing_msg;
	private static int knowing_index=-1;
	
	private int state;
	private ChooseItemDialog chooseBox;
	private NoteCatagory3 noteCategory;
	
	
	//write File
	private File mainDirectory;
	private long timestamp = 0;
	private QuestionFile questionFile; 
	
	//Listener

	private EndOnClickListener endOnClickListener;
	private NextOnClickListener nextOnClickListener;
	private GoResultOnClickListener goResultOnClickListener;
	private GoCopingToResultOnClickListener goCopingToResultOnClickListener;
	private MyOnPageChangeListener myOnPageChangeListener;
	
	private int day=0;
	private int timeslot=0; //TODO: default
	private int type;
	private int items, items2;
	private int impact;
	private int moodNum = 20;
	private boolean[] moodFlag = new boolean[20];
	private String description;
	private boolean viewshow = false, viewshow2 = false, historyViewshow = false;
	
	private CustomScrollView sv;
	private boolean done = false;
	
	public static final int STATE_TEST = 0;
	public static final int STATE_NOTE = 1;
	public static final int STATE_COPE = 2;
	public static final int STATE_KNOW = 3;
	
	private static final String[] Timeslot_str = {"上午", "下午", "晚上"};
	private static final String[] Date_str = {"今天", "昨天", "前天"};
	protected static final int RESULT_SPEECH = 0;
	
	private static final int[] Coping_list = {R.array.coping_list0,R.array.coping_list1,
			R.array.coping_list2,R.array.coping_list3,R.array.coping_list4,R.array.coping_list5,
			R.array.coping_list6,R.array.coping_list7,R.array.coping_list8};
	
	private static Typeface wordTypefaceBold = Typefaces.getWordTypefaceBold();
	private static Typeface wordTypeface = Typefaces.getWordTypeface();
	
	private ImageView speech_button, check_bar, check_out;
	public static EditText thinking_text;
	private boolean notification_yes, testing = false;
	public static boolean testFinished = false;
	
	private DatabaseControl db;

	private ListView historyListView;
	private DropEditText dropEditText;
	private ImageView  showHistory;
	private boolean showResultToast;

	public AddNoteDialog2(TestQuestionCaller2 testQuestionCaller, RelativeLayout mainLayout, Activity activity){
		this.activity = activity;
		this.testQuestionCaller = testQuestionCaller;
		this.context = App.getContext();
		this.inflater = (LayoutInflater) context
				.getSystemService(Context.LAYOUT_INFLATER_SERVICE);
		this.mainLayout = mainLayout;
		this.addNoteDialog = this;

		resource = context.getResources();
		
		coping_msg = resource.getStringArray(R.array.coping_list);
		
		knowing_msg = context.getResources().getStringArray(R.array.knowing_list);
		Random rand = new Random();
		if( knowing_index < 0 )
			knowing_index = rand.nextInt(knowing_msg.length);
		//view = inflater.inflate(R.layout.fragment_note, container, false);
		endOnClickListener = new EndOnClickListener();
		nextOnClickListener = new NextOnClickListener();
		goResultOnClickListener = new GoResultOnClickListener();
		goCopingToResultOnClickListener = new GoCopingToResultOnClickListener();
				
		noteCategory = new NoteCatagory3();
		db = new DatabaseControl();
	}
	
	protected void setting() {
		
		day = 0;
		type = -1;
		items = -1;
		impact = 0 ;
		description = "";
		showResultToast= false;

		boxLayout = (RelativeLayout) inflater.inflate(
				R.layout.note, null);
		boxLayout.setVisibility(View.INVISIBLE);
		title_layout = (LinearLayout) boxLayout.findViewById(R.id.note_title_layout);
		main_layout = (LinearLayout) boxLayout.findViewById(R.id.note_main_layout);
		
		
		
		bottom_layout = (LinearLayout) boxLayout.findViewById(R.id.note_bottom_layout);
		sv = (CustomScrollView) boxLayout.findViewById(R.id.note_main_scroll);
		
		//
		
		//Title View
		//View title = BarButtonGenerator.createAddNoteView(new DateSelectedListener(), new TimeslotSelectedListener() );
		
		title = (LinearLayout) inflater.inflate(
				R.layout.bar_addnote2, null);
		title_txt = (TextView)title.findViewById(R.id.note_title);
		date_layout = (LinearLayout) title.findViewById(R.id.note_date_layout);
		timeslot_layout = (LinearLayout) title.findViewById(R.id.note_timeslot_layout);
		date_txt = (TextView)title.findViewById(R.id.note_tx_date);
		timeslot_txt= (TextView)title.findViewById(R.id.note_tx_timeslot);
		
		checkAndSetTimeSlot();
			
		title_txt.setTypeface(wordTypefaceBold);
		date_txt.setTypeface(wordTypefaceBold);
		timeslot_txt.setTypeface(wordTypefaceBold);
		
		
		date_layout.setOnClickListener(new OnClickListener(){
			

			@Override
			public void onClick(View v) {
				
				ClickLog.Log(ClickLogId.DAYBOOK_ADDNOTE_SELECT_DATE);
				listView.setVisibility(View.GONE);
				viewshow = false;
				setEnabledAll(boxLayout, false);
				
				chooseBox = new ChooseItemDialog(addNoteDialog,boxLayout, 1, day, testing);
				chooseBox.initialize();
				chooseBox.show();
			}
			
		});
		
		timeslot_layout.setOnClickListener(new OnClickListener(){
			

			@Override
			public void onClick(View v) {
				ClickLog.Log(ClickLogId.DAYBOOK_ADDNOTE_SELECT_SLOT);
				listView.setVisibility(View.GONE);
				setEnabledAll(boxLayout, false);
				chooseBox = new ChooseItemDialog(addNoteDialog,boxLayout, 2, day,testing);
				chooseBox.initialize();
				chooseBox.show();			
			}
			
		});
		
		title_layout.addView(title);
		
		
		
		center_layout = (LinearLayout) inflater.inflate(
				R.layout.note_main2, null);
		text_self = (TextView)center_layout.findViewById(R.id.text_self);
	    text_other = (TextView)center_layout.findViewById(R.id.text_other);
	    
		text_self.setTypeface(wordTypefaceBold);
	    text_other.setTypeface(wordTypefaceBold);
	    
		text_self.setTextColor(resource.getColor(R.color.blue));    
	    text_self.setOnClickListener(new MyClickListener(0));
	    text_other.setOnClickListener(new MyClickListener(1));
	    
		initTypePager();
			
		//Type
		LinearLayout type_layout = (LinearLayout) inflater.inflate(
				R.layout.bar_type_name, null);
		
		TextView type_title = (TextView)type_layout.findViewById(R.id.type_title);
		type_title.setText("情境類型：");
		type_title.setTypeface(wordTypefaceBold);
		typetext = (TextView)type_layout.findViewById(R.id.type_content);
		typetext.setTypeface(wordTypefaceBold);

		
		//Spinner
		LinearLayout spinner_layout = (LinearLayout) inflater.inflate(
				R.layout.bar_spinner, null);
			
		//sp_item = (Spinner)spinner_layout.findViewById(R.id.spinner_content);
		//SetItem(sp_item, R.array.item_select);
		
		TextView spin_title = (TextView)spinner_layout.findViewById(R.id.spinner_title);
		spin_title.setText("發生情境：");
		spin_title.setTypeface(wordTypefaceBold);
		
		sp_content = (TextView)spinner_layout.findViewById(R.id.spinner_content);
		sp_content.setText("");
		sp_content.setTypeface(wordTypefaceBold);
		sp_content.setOnClickListener(new OnClickListener(){

			@Override
			public void onClick(View v) {
				//listView.setVisibility(View.VISIBLE);
				if(type != -1)
					listViewShowHide(listView);
			}
								
		});
		
		listView = (ListView)spinner_layout.findViewById(R.id.item_listview);
		
		
		//Impact
		LinearLayout impact_layout = (LinearLayout) inflater.inflate(
				R.layout.bar_impact, null);
		impactSeekBar=(SeekBar)impact_layout.findViewById(R.id.impact_seek_bar);
		impactSeekBar.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener(){

			@Override
			public void onProgressChanged(SeekBar seekBar, int progress,
					boolean fromUser) {
				ClickLog.Log(ClickLogId.DAYBOOK_ADDNOTE_IMPACT);
				done = true;
			}

			@Override
			public void onStartTrackingTouch(SeekBar seekBar) {				
			}

			@Override
			public void onStopTrackingTouch(SeekBar seekBar) {				
			}
			
		});
		TextView impact_title = (TextView)impact_layout.findViewById(R.id.impact_title);
		impact_title.setTypeface(wordTypefaceBold);
		
		
		
		//當時的行為
		LinearLayout description_thinking_layout = (LinearLayout) inflater.inflate(
				R.layout.bar_edit_thinking, null);
		
		TextView thinking_title = (TextView)description_thinking_layout.findViewById(R.id.edit_thinking_title);
		//EditText thinking_content =  (EditText)description_thinking_layout.findViewById(R.id.edit_thinking_content);
		thinking_title.setTypeface(wordTypefaceBold);
		thinking_title.setText("當發生「上述情境」時\n我做了什麼？");
		
		speech_button = (ImageView) description_thinking_layout.findViewById(R.id.speech_to_text);
		showHistory = (ImageView) description_thinking_layout.findViewById(R.id.show_text_list1);
		thinking_text = (EditText) description_thinking_layout.findViewById(R.id.edit_thinking_content);

		historyListView = (ListView) description_thinking_layout.findViewById(R.id.listview_page1);
		
		thinking_text.setText("");
		
		
		if(!NetworkCheck.networkCheck())
		{
			MainActivity.networkState = false;
			speech_button.setImageResource(R.drawable.speech_icon_unable);
		}
		else
		{
			MainActivity.networkState = true;
			speech_button.setImageResource(R.drawable.speech_icon);
		}
		
		speech_button.setOnClickListener(new OnClickListener(){

			@Override
			public void onClick(View v) {
				if (!NetworkCheck.networkCheck()) {
					MainActivity.networkState = false;
					speech_button.setImageResource(R.drawable.speech_icon_unable);
				}
				else
				{
					MainActivity.networkState = true;
					speech_button.setImageResource(R.drawable.speech_icon);
				}
					
				
				if(!MainActivity.networkState){
					CustomToastSmall.generateToast("請先開啟網路");
					return;
				}
				

				
				MainActivity.networkState = true;
				speech_button.setImageResource(R.drawable.speech_icon);
				
				Intent intent = new Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH);

				intent.putExtra(RecognizerIntent.EXTRA_LANGUAGE_MODEL, "zh-TW"); // en-US
				try {
					activity.startActivityForResult(intent, RESULT_SPEECH);
				 } catch (ActivityNotFoundException a) {
	                    Toast t = Toast.makeText(activity.getApplicationContext(),
	                            "Opps! Your device doesn't support Speech to Text",
	                            Toast.LENGTH_SHORT);
	                    t.show();	       
	             }
				
			}
								
		});

		dropEditText = new DropEditText(historyListView, showHistory, thinking_text, 1, 0, context);
		dropEditText.setting();
		
		//Description
		LinearLayout discription_layout = (LinearLayout) inflater.inflate(
				R.layout.bar_description, null);
		
		TextView dec_title = (TextView)discription_layout.findViewById(R.id.description_title);
		dec_title.setText("當我感覺＿＿，我「做了上述行為」：");
		dec_title.setTypeface(wordTypefaceBold);
		
		/*TODO*/
		edtext = (TextView)discription_layout.findViewById(R.id.description_content);
		edtext.setText("");
		edtext.setTypeface(wordTypefaceBold);
		edtext.setOnClickListener(new OnClickListener(){

			@Override
			public void onClick(View v) {

	        	
				listViewShowHide(listView2);
				
			}
								
		});
		
		listView2 = (ListView)discription_layout.findViewById(R.id.item_listview2);
		
		SetListItemFeeling();
		
		for(int i = 0;i < 10; ++i)
    		moodFlag[i] = false;
		
		//提醒
		/*LinearLayout notification_layout = (LinearLayout) inflater.inflate(
				R.layout.bar_notification, null);
		
		check_bar = (ImageView) notification_layout.findViewById(R.id.check_in);
		check_out = (ImageView) notification_layout.findViewById(R.id.check_out);
		
		check_bar.setImageResource(R.drawable.check_in);
		notification_yes = false;
		check_out.setOnClickListener(new OnClickListener(){

			@Override
			public void onClick(View v) {
				if(notification_yes)
					check_bar.setImageResource(0);
				else
					check_bar.setImageResource(R.drawable.check_in);
				
				notification_yes = !notification_yes;
			}
			
			
		});*/
		
		
		
//		edtext.setOnFocusChangeListener(new OnFocusChangeListener() {
//
//		    @Override
//		    public void onFocusChange(View v, boolean hasFocus) {
//		        if (hasFocus) {
//		        // Always use a TextKeyListener when clearing a TextView to prevent android
//		        // warnings in the log
//		        	Log.i(TAG, "EditText");
//		        	ClickLog.Log(ClickLogId.DAYBOOK_ADDNOTE_DESCRIPTION);
//		        }
//		    }
//		});
		/*edtext.setOnTouchListener(new OnTouchListener() {
		    @Override
		    public boolean onTouch(View v, MotionEvent event) {
		        if (event.getAction() == MotionEvent.ACTION_UP) {
		        	Log.i(TAG, "EditText");
		        	ClickLog.Log(ClickLogId.DAYBOOK_ADDNOTE_DESCRIPTION);
		        }
		        return false;
		    }
		});*/
		
		//Bottom View
		View bottom;
		//if(!testing)
			bottom = BarButtonGenerator.createThreeButtonView(R.string.cancel, R.string.end_record,R.string.next_step, 
														new CancelOnClickListener(), endOnClickListener, nextOnClickListener);
		//else
		//	bottom = BarButtonGenerator.createTwoButtonView(R.string.end_record,R.string.next_step, 
		//			 									endOnClickListener, nextOnClickListener);
		
		main_layout.addView(center_layout);
		main_layout.addView(type_layout);
		main_layout.addView(spinner_layout);
		main_layout.addView(description_thinking_layout);
		main_layout.addView(discription_layout);
		main_layout.addView(impact_layout);
		//main_layout.addView(notification_layout);
		
//		center_layout.setOnClickListener(new OnClickListener(){
//
//			@Override
//			public void onClick(View v) {
//				listView.setVisibility(View.GONE);
//			}
//			
//		});
		
//		main_layout.setOnClickListener(new OnClickListener(){
//
//			@Override
//			public void onClick(View v) {
//				listView.setVisibility(View.GONE);
//			}
//			
//		});
		
		//main_layout.addView(bottom);
		bottom_layout.addView(bottom);
		title_layout.bringToFront();
		//bottom_layout.setVisibility(View.GONE);
		bottom_layout.bringToFront();
		//main_layout.addView(bottom);
	}
	
	
	public void setEnabledAll(View v, boolean enabled) {
	    v.setEnabled(enabled);
	    v.setFocusable(enabled);

	    if(v instanceof ViewGroup) {
	        ViewGroup vg = (ViewGroup) v;
	        for (int i = 0; i < vg.getChildCount(); i++)
	            setEnabledAll(vg.getChildAt(i), enabled);
	    }
	}
	
	private void listViewShowHide(ListView listview){
		if(listview == listView){
			if(!viewshow)
				listview.setVisibility(View.VISIBLE);
			else
				listview.setVisibility(View.GONE);
			viewshow = !viewshow;
		}
		if(listview == listView2){
			
			if(!viewshow2)
				listview.setVisibility(View.VISIBLE);
			else
				listview.setVisibility(View.GONE);
			viewshow2 = !viewshow2;
		}
	}
	
	/*public void copingSetting(){
		//boxLayout = (RelativeLayout) inflater.inflate(R.layout.activity_qtip, null);
		//mainLayout.addView(boxLayout);
		state = STATE_COPE;
		PreferenceControl.setAfterTestState(STATE_COPE);
		
		title_layout.removeAllViews();
		main_layout.removeAllViews();
		bottom_layout.removeAllViews();
		bottom_layout.setVisibility(View.VISIBLE);
		
		//Title View
		LinearLayout layout = (LinearLayout) inflater.inflate(
				R.layout.bar_addnote, null);
		
		note_title = (TextView) layout
				.findViewById(R.id.note_title);
		//Spinner sp_date = (Spinner)layout.findViewById(R.id.note_tx_date);
	    //Spinner sp_timeslot = (Spinner)layout.findViewById(R.id.note_sp_timeslot);
	    
	    note_title.setTypeface(wordTypefaceBold);
	    note_title.setTextColor(resource.getColor(R.color.text_gray2));
	    note_title.setText(R.string.countdown);
	    
	    sp_date.setVisibility(View.INVISIBLE);
	    sp_timeslot.setVisibility(View.INVISIBLE);
		title_layout.addView(layout);
		
		
		//View title = BarButtonGenerator.createWaitingTitle();
		
		
		center_layout = (LinearLayout) inflater.inflate(R.layout.knowledge, null);
		tv_knowdlege = (TextView)center_layout.findViewById(R.id.qtip_tv_tips);
		tv_title = (TextView)center_layout.findViewById(R.id.text_knowing_title);
		
		tv_title.setText(R.string.coping_page);
		
		Random rand = new Random();
		int idx = rand.nextInt(coping_msg.length);
		tv_knowdlege.setText(coping_msg[idx]);
		main_layout.addView(center_layout);
		
		View bottom = BarButtonGenerator.createOneButtonView( R.string.Iknow, endOnClickListener );
		bottom_layout.addView(bottom);
		
	}*/
	public void copingSetting(){
		//boxLayout = (RelativeLayout) inflater.inflate(R.layout.activity_qtip, null);
		//mainLayout.addView(boxLayout);
		
		boolean testFail = PreferenceControl.isTestFail();
		boolean runService = PreferenceControl.getResultServiceRun();
		
		
		ClickLog.Log(ClickLogId.TEST_COPE_ENTER);
		state = STATE_COPE;
		PreferenceControl.setAfterTestState(STATE_COPE);
		
		title_layout.removeAllViews();
		main_layout.removeAllViews();
		bottom_layout.removeAllViews();
		
		bottom_layout.setVisibility(View.VISIBLE);
		
		//Title View
		LinearLayout layout = (LinearLayout) inflater.inflate(
				R.layout.bar_addnote3, null);
		
		note_title = (TextView) layout
				.findViewById(R.id.note_title);
	    
	    note_title.setTypeface(wordTypefaceBold);
	    note_title.setTextColor(context.getResources().getColor(R.color.text_gray2));
	    
	    if(!testFail && !runService){
	    	note_title.setText(R.string.test_done);
	    }
	    else{
	    	note_title.setText(R.string.countdown);
	    }
	      
		title_layout.addView(layout);
		
		
		//View title = BarButtonGenerator.createWaitingTitle();
		
		
		center_layout = (LinearLayout) inflater.inflate(R.layout.knowledge, null);
		tv_knowdlege = (TextView)center_layout.findViewById(R.id.qtip_tv_tips);
		tv_title = (TextView)center_layout.findViewById(R.id.text_knowing_title);
		
		tv_title.setText(R.string.coping_page);
		
		coping_msg = context.getResources().getStringArray(Coping_list[type]);
		Random rand = new Random();
		int idx = rand.nextInt(coping_msg.length);
		tv_knowdlege.setText(Html.fromHtml(coping_msg[idx]));
		main_layout.addView(center_layout);
		
		
		View bottom;
		if(!testFail && !runService){
			bottom = BarButtonGenerator.createOneButtonView( R.string.Iknow, endOnClickListener );
		}
		else{
			bottom = BarButtonGenerator.createOneButtonView( R.string.Iknow, endOnClickListener );
		}
		bottom_layout.addView(bottom);
		
	}
	
	public void knowingSetting(){
		if(title_layout == null)
		{
			initialize();
			show();
		}
		
		ClickLog.Log(ClickLogId.TEST_KOWING_ENTER);
		state = STATE_KNOW;
		PreferenceControl.setAfterTestState(STATE_KNOW);
		//MainActivity.getMainActivity().enableTabAndClick(true);

		title_layout.removeAllViews();
		main_layout.removeAllViews();
		bottom_layout.removeAllViews();
		
		LinearLayout layout = (LinearLayout) inflater.inflate(
				R.layout.bar_addnote3, null);
		
		note_title = (TextView) layout
				.findViewById(R.id.note_title);

	    
	    note_title.setTypeface(wordTypefaceBold);
	    note_title.setTextColor(context.getResources().getColor(R.color.text_gray2));
	    note_title.setText(R.string.countdown);
	    

		title_layout.addView(layout);
		
		//View title = BarButtonGenerator.createWaitingTitle();
		//title_layout.addView(title);
		
		View bottom = BarButtonGenerator.createTwoButtonView(R.string.last, R.string.next_one, new CancelOnClickListener(), endOnClickListener);
		bottom_layout.addView(bottom);
		//main_layout.removeView(center_layout);
		center_layout = (LinearLayout) inflater.inflate(R.layout.knowledge, null);
		tv_knowdlege = (TextView)center_layout.findViewById(R.id.qtip_tv_tips);
		//tv_knowdlege.setText(knowing_msg[knowing_index]); 
		tv_knowdlege.setText(Html.fromHtml(knowing_msg[knowing_index]));
		
		tv_title = (TextView)center_layout.findViewById(R.id.text_knowing_title);
		tv_title.setText(R.string.knowledge);
		
		main_layout.addView(center_layout);
		
		main_layout.getLayoutParams().height = center_layout.getLayoutParams().height;
		
	}
	
	
	public void copingSettingToResult(){
		//boxLayout = (RelativeLayout) inflater.inflate(R.layout.activity_qtip, null);
		//mainLayout.addView(boxLayout);
		state = STATE_COPE;
		PreferenceControl.setAfterTestState(STATE_COPE);
		
		title_layout.removeAllViews();
		main_layout.removeAllViews();
		bottom_layout.removeAllViews();
		
		LinearLayout layout = (LinearLayout) inflater.inflate(
				R.layout.bar_addnote, null);
		
		note_title = (TextView) layout
				.findViewById(R.id.note_title);
		//Spinner sp_date = (Spinner)layout.findViewById(R.id.note_tx_date);
	    //Spinner sp_timeslot = (Spinner)layout.findViewById(R.id.note_sp_timeslot);
	    
	    note_title.setTypeface(wordTypefaceBold);
	    note_title.setTextColor(resource.getColor(R.color.text_gray2));
	    //note_title.setText(R.string.countdown);
	    note_title.setText(R.string.test_done);
	    
	    sp_date.setVisibility(View.INVISIBLE);
	    sp_timeslot.setVisibility(View.INVISIBLE);
		title_layout.addView(layout);
		
		//View title = BarButtonGenerator.createWaitingTitle();
		//title_layout.addView(title);
		
		center_layout = (LinearLayout) inflater.inflate(R.layout.knowledge, null);
		tv_knowdlege = (TextView)center_layout.findViewById(R.id.qtip_tv_tips);
		tv_title = (TextView)center_layout.findViewById(R.id.text_knowing_title);
		
		tv_title.setText(R.string.coping_page);
		
		Random rand = new Random();
		int idx = rand.nextInt(coping_msg.length);
		tv_knowdlege.setText(coping_msg[idx]);
		main_layout.addView(center_layout);
		
		View bottom = BarButtonGenerator.createOneButtonView( R.string.go_result, goResultOnClickListener );
		bottom_layout.addView(bottom);
		
	}
	
	
	
	public void setResult(){
		testFinished  = true;

		//Toast.makeText(context, "倒數結束", Toast.LENGTH_SHORT).show();

		if(state == STATE_NOTE){
			if(!showResultToast)
				Toast.makeText(context, "請完成新增記事以查看檢測結果", Toast.LENGTH_SHORT).show();
			showResultToast = true;
			//View bottom = BarButtonGenerator.createTwoButtonView(R.string.cancel, R.string.ok, goCopingToResultOnClickListener, goCopingToResultOnClickListener);
			//bottom_layout.addView(bottom);
		}
		else if(state == STATE_COPE){
			note_title.setText(R.string.test_done);
		}
		else if(state == STATE_KNOW){
			if(bottom_layout != null)
				bottom_layout.removeAllViews();
			Toast.makeText(context, "請點選以查看檢測結果", Toast.LENGTH_SHORT).show();
			note_title.setText(R.string.test_done);
			View bottom = BarButtonGenerator.createOneButtonView( R.string.go_result, goResultOnClickListener );
			bottom_layout.addView(bottom);
		}	
	}
	
	
	/** Initialize the dialog */
	public void initialize() {
		
		setting();
	    mainLayout.addView(boxLayout);
		
	}
	
	/** show the dialog */
	public void show() {
		//if(testing)
			state = STATE_NOTE;
		ClickLog.Log(ClickLogId.DAYBOOK_ADDNOTE_ENTER);
		//PreferenceControl.setAfterTestState(STATE_NOTE);
		//questionLayout.setVisibility(View.VISIBLE);
		
		//MainActivity.getMainActivity().enableTabAndClick(false);
		if(boxLayout != null)
			boxLayout.setVisibility(View.VISIBLE);
		
		//chooseBox = new ChooseItemDialog(boxLayout, 1);
		//chooseBox.initialize();
		//chooseBox.show();

	}
	
	/** remove the dialog and release the resources */
	public void clear() {
		if (mainLayout != null && boxLayout != null
				&& boxLayout.getParent() != null
				&& boxLayout.getParent().equals(mainLayout))
			mainLayout.removeView(boxLayout);
	}
	
	/** close the dialog */
	
	public void close() {
		ClickLog.Log(ClickLogId.DAYBOOK_ADDNOTE_LEAVE);
		testQuestionCaller.resetView();
		MainActivity.getMainActivity().enableTabAndClick(true);
		Log.d("GGG", "resume 3");
		if (boxLayout != null)
			boxLayout.setVisibility(View.INVISIBLE);
	}
	
	
	
	private void SetListItem2(int type, boolean clearFlag){
		ClickLog.Log(ClickLogId.DAYBOOK_ADDNOTE_SELECT_TYPE + type);
		//ArrayAdapter adapter = ArrayAdapter.createFromResource(context, array, android.R.layout.simple_list_item_1);
		if(clearFlag)
		{
			items = -1;
			sp_content.setText(""); //TODO: 假如點到同一個不要清掉
		}
		//ArrayAdapter adapter = ArrayAdapter.createFromResource(context, array, R.layout.my_listitem);
		String[] type1 = PreferenceControl.getType1();
		switch(type){
		case 1:
			type1 = PreferenceControl.getType1();
			break;
		case 2:
			type1 = PreferenceControl.getType2();
			break;
		case 3:
			type1 = PreferenceControl.getType3();
			break;
		case 4:
			type1 = PreferenceControl.getType4();
			break;
		case 5:
			type1 = PreferenceControl.getType5();
			break;
		case 6:
			type1 = PreferenceControl.getType6();
			break;
		case 7:
			type1 = PreferenceControl.getType7();
			break;
		case 8:
			type1 = PreferenceControl.getType8();
			break;
	
		}
				
		String[] after = clean(type1);
		Log.d("GG", "after length : "+after.length);
		for(int i = 0; i < after.length; i++)
		{
			Log.d("GG", (i+1) + ":"+after[i]);
		}
		
		ArrayAdapter adapter = new ArrayAdapter<String>(context, R.layout.my_listitem, after);
		listView.setAdapter(adapter);
		listView.setOnItemClickListener(new OnItemClickListener(){

		   @Override
		   public void onItemClick(AdapterView<?> parent, View view, int position, long id){
			   ClickLog.Log(ClickLogId.DAYBOOK_ADDNOTE_SELECT_ITEM);
			   TextView c = (TextView) view.findViewById(android.R.id.text1);
			    String playerChanged = c.getText().toString();
			    
			    items = noteCategory.myNewHashMap.get(playerChanged);
			   	dropEditText.setItem(items);
			   	dropEditText.setting();
				Log.d(TAG, items+"");
			    //Toast.makeText(Settings.this,playerChanged, Toast.LENGTH_SHORT).show();  
			 sp_content.setText(playerChanged);
			 listView.setVisibility(View.GONE);
			 viewshow = false;
		   }
		   
		});
		setListViewHeightBasedOnItems(listView);
		if(clearFlag)
		{
			listView.setVisibility(View.VISIBLE);
			viewshow = true;
			sv.smoothScrollTo(0 , (int)convertDpToPixel((float)200));
		}
		
		//.setOnItemSelectedListener(new SpinnerXMLSelectedListener());
	}
	 public static String[] clean(final String[] v) {
		    List<String> list = new ArrayList<String>(Arrays.asList(v));
		    list.removeAll(Collections.singleton(""));
		    list.removeAll(Collections.singleton(null));
		    return list.toArray(new String[list.size()]);
		}
	
	
	//把所選取的結果送出 
	class EndOnClickListener implements View.OnClickListener{
		public void onClick(View v){
			
			//else{
				Log.d(TAG, items+" "+impact);
				if(state == STATE_NOTE){
					if(type <= 0 || items < 100){
						//CustomToastSmall.generateToast(R.string.note_check);
						//Toast.makeText(context, R.string.note_check ,Toast.LENGTH_SHORT).show();
						CustomToastSmall.generateToast(R.string.note_check);
					}
					else{
						if(listView.getVisibility() == View.VISIBLE){
							//Toast.makeText(context, "請選擇項目再送出", Toast.LENGTH_SHORT).show();
							CustomToastSmall.generateToast("請選擇項目再送出");
							listView.setVisibility(View.GONE);
							viewshow = false; 
						}
						else{
							if(thinking_text.getText().toString().length() == 0 || edtext.getText().toString().length() == 0)
							{
								CustomToastSmall.generateToast("請先填寫完畢");
								return;
							}
							
							if(!done){
								//Toast.makeText(context, "確定要送出結果嗎?" ,Toast.LENGTH_SHORT).show();
								CustomToastSmall.generateToast("確定要送出結果嗎?");
								done = true;
								return;
							}
							
							NotifyThinkingDialog notifyThinkingDialog;
							notifyThinkingDialog = new NotifyThinkingDialog(addNoteDialog, (RelativeLayout) mainLayout);
							notifyThinkingDialog.initialize();
							notifyThinkingDialog.show(1);
							/*ClickLog.Log(ClickLogId.DAYBOOK_ADDNOTE_CONFIRM);
							
							impact = impactSeekBar.getProgress();
							testQuestionCaller.writeQuestionFile(day, timeslot, type, items, impact, thinking_text.getText().toString(),edtext.getText().toString(),null,0 ,0);
							
							
							if(testing)
							{
								copingSetting();
								
							}
							else
							{
								close();
								clear();
							}
							
							CustomToast.generateToast(R.string.add_note_no_thinking, 1);
							AddScore preScore = db.getLastestAddScore();
							AddScore nowScore = new AddScore(System.currentTimeMillis(), 1, preScore.getAccumulation()+1, "填寫事件");
							db.insertAddScore(nowScore);*/
							//copingSetting();
							//testQuestionCaller.resetView();
							
						}
					}
					
				}
				
				else if(state == STATE_COPE){
					ClickLog.Log(ClickLogId.TEST_COPING_CONFIRM);
					
					Log.d("GG", "before show");
					questionBox = new QuestionIdentityDialog((RelativeLayout) mainLayout);
					questionBox.initialize();
					questionBox.show(1);
					Log.d("GG", "after show");
					
					if(!testFinished)
						knowingSetting();

				}
				else if(state == STATE_KNOW){
					ClickLog.Log(ClickLogId.TEST_KOWING_NEXT);
					knowing_index++;
					if(knowing_index>=knowing_msg.length)
						knowing_index-=knowing_msg.length;
					tv_knowdlege.setText(Html.fromHtml(knowing_msg[knowing_index]));
					//tv_knowdlege.setText(DBTip.inst.getTip());
				}
			//}
	    }
	}
	
	//把所選取的結果取消
	class CancelOnClickListener implements View.OnClickListener{
		public void onClick(View v){
			
			ClickLog.Log(ClickLogId.DAYBOOK_ADDNOTE_CANCEL);
			if(state == STATE_NOTE){
				if(testing)
				{
					knowingSetting();
					questionBox = new QuestionIdentityDialog((RelativeLayout) mainLayout);
					questionBox.initialize();
					questionBox.show(1);
				}
				else
				{
					close();
					clear();
					testQuestionCaller.resetView();
				}
				
				
			}
			else{
				ClickLog.Log(ClickLogId.TEST_KOWING_LAST);
				knowing_index--;
				if(knowing_index<0)
					knowing_index+=knowing_msg.length;
				tv_knowdlege.setText(Html.fromHtml(knowing_msg[knowing_index]));
				//tv_knowdlege.setText(DBTip.inst.getTip());
			}

		}
	}
	
	//到下一步
	class NextOnClickListener implements View.OnClickListener{
		public void onClick(View v){
			//CustomToastSmall.generateToast("下一步");
	
			//else{
				Log.d(TAG, items+" "+impact);
				if(state == STATE_NOTE){
					if(type <= 0 || items < 100){
						//CustomToastSmall.generateToast(R.string.note_check);
						//Toast.makeText(context, R.string.note_check ,Toast.LENGTH_SHORT).show();
						CustomToastSmall.generateToast(R.string.note_check);
					}
					else{
						if(listView.getVisibility() == View.VISIBLE){
							//Toast.makeText(context, "請選擇項目再送出", Toast.LENGTH_SHORT).show();
							CustomToastSmall.generateToast("請選擇項目再送出");
							listView.setVisibility(View.GONE);
							viewshow = false;
						}
						else{
							ClickLog.Log(ClickLogId.DAYBOOK_ADDNOTE_CONFIRM);
							
							impact = impactSeekBar.getProgress();
							
							
							//set next page
							LinearLayout temp_layout1 = (LinearLayout) inflater.inflate(
									R.layout.bar_description_date, null);
							TextView date_text = (TextView)temp_layout1.findViewById(R.id.description_date_content);
							
							LinearLayout temp_layout2 = (LinearLayout) inflater.inflate(
									R.layout.bar_description_date, null);
							TextView event_text = (TextView)temp_layout2.findViewById(R.id.description_event_content);
							
							LinearLayout temp_layout3 = (LinearLayout) inflater.inflate(
									R.layout.bar_description_date, null);
							TextView mood_text = (TextView)temp_layout3.findViewById(R.id.description_mood_content);
							
							Calendar c = Calendar.getInstance();
							c.add(Calendar.DAY_OF_MONTH, day*-1);
							
							int _year =  c.get(Calendar.YEAR);
							int _month = c.get(Calendar.MONTH) + 1;         
							int _day = c.get(Calendar.DAY_OF_MONTH); 
							
							if(thinking_text.getText().toString().length() == 0 || edtext.getText().toString().length() == 0)
							{
								CustomToastSmall.generateToast("請先填寫完畢");
								return;
							}
							
							if(!done){
								//Toast.makeText(context, "確定要送出結果嗎?" ,Toast.LENGTH_SHORT).show();
								CustomToastSmall.generateToast("確定要到下一步嗎?");
								done = true;
								return;
							}
							
							
							//DaybookFragment.addNoteStep = 1;
							notePage2 = new AddNoteDialogThinking(testQuestionCaller, mainLayout, activity,addNoteDialog,false, 0, items);
							
							String ts = thinking_text.getText().toString();
							
							notePage2.initialize();
							notePage2.setAllText( _year+"年"+_month+"月"+_day+"日",sp_content.getText().toString(), edtext.getText().toString(), ts);
							notePage2.setAddNoteDetail(day, timeslot, type, items, impact, thinking_text.getText().toString(),edtext.getText().toString());
							
							notePage2.setIsTesting(testing, type);
							
							notePage2.show();
							
					
							
						}
					}
					
				}
				
				else if(state == STATE_COPE){
					//knowingSetting();
					//
					//testQuestionCaller.resetView();
					
					close();
					clear();
				
				}
				
			//}
	    }
	}
	
	
	class GoResultOnClickListener implements View.OnClickListener{
		public void onClick(View v){
			//CustomToast.generateToast(R.string.after_test_pass, 2);
			MainActivity.getMainActivity().changeTab(1);
			
	    }
	}
	
	class GoCopingToResultOnClickListener implements View.OnClickListener{
		public void onClick(View v){
			
			
			if(state == STATE_NOTE){
				impact = impactSeekBar.getProgress();
				//testQuestionCaller.writeQuestionFile(day, timeslot, type, items, impact, edtext.getText().toString());
			
				//Log.d(TAG, items+" "+impact);

				copingSettingToResult();
			}

	    }
	}
	
	private void initTypePager(){
	    vPager = (ViewPager) center_layout.findViewById(R.id.viewpager);
	    vPager.setOnPageChangeListener(new MyOnPageChangeListener());
	    
		//LayoutInflater li = LayoutInflater.from(context); //getLayoutInflater();
		ArrayList<View> aList = new ArrayList<View>();
		aList.add(inflater.inflate(R.layout.typepager_self, null));
		aList.add(inflater.inflate(R.layout.typepager_other, null));
		TypePageAdapter mAdapter = new TypePageAdapter(aList);		
		vPager.setAdapter(mAdapter);
	}
	
	
	public class TypePageAdapter extends PagerAdapter{
		
		private ArrayList<View> viewLists;	

		public TypePageAdapter() {}	
		public TypePageAdapter(ArrayList<View> viewLists)
		{
			super();
			this.viewLists = viewLists;
		}
		
		@Override
		public int getCount() {
			return viewLists.size();
		}
		
		@Override
		public boolean isViewFromObject(View view, Object object) {
			return view == object;
		}
		
		private void resetView(){
			iv_smile.setImageResource(R.drawable.type_icon3);
			iv_not_good.setImageResource(R.drawable.type_icon2);
			iv_urge.setImageResource(R.drawable.type_icon5);
			iv_cry.setImageResource(R.drawable.type_icon1);
			iv_try.setImageResource(R.drawable.type_icon4);
			
			iv_social.setImageResource(R.drawable.type_icon7);
			iv_playing.setImageResource(R.drawable.type_icon8);
			iv_conflict.setImageResource(R.drawable.type_icon6);
		}
		
		/** 初始化Type*/
		@Override
		public Object instantiateItem(ViewGroup container, int position) {
			container.addView(viewLists.get(position));
			if(position == 0){
				iv_smile = (ImageView) center_layout.findViewById(R.id.vts_iv_smile);
				iv_not_good = (ImageView) center_layout.findViewById(R.id.vts_iv_not_good);
				iv_urge = (ImageView) center_layout.findViewById(R.id.vts_iv_urge);
				iv_cry = (ImageView) center_layout.findViewById(R.id.vts_iv_cry);
				iv_try = (ImageView) center_layout.findViewById(R.id.vts_iv_try);
				iv_self_others_bar = (ImageView) center_layout.findViewById(R.id.self_others_bar);
				iv_smile.setOnClickListener(SelectItem);
				iv_not_good.setOnClickListener(SelectItem);
				iv_urge.setOnClickListener(SelectItem);
				iv_cry.setOnClickListener(SelectItem);
				iv_try.setOnClickListener(SelectItem);
			}else{
				iv_social = (ImageView) center_layout.findViewById(R.id.vts_iv_social);
				iv_playing = (ImageView) center_layout.findViewById(R.id.vts_iv_playing);
				iv_conflict = (ImageView) center_layout.findViewById(R.id.vts_iv_conflict);
				iv_self_others_bar = (ImageView) center_layout.findViewById(R.id.self_others_bar);
				iv_social.setOnClickListener(SelectItem);
				iv_playing.setOnClickListener(SelectItem);
				iv_conflict.setOnClickListener(SelectItem);
			}
			//Log.d("FORTEST", "aabb");
			
			return viewLists.get(position);	
		}

		@Override
		public void destroyItem(ViewGroup container, int position, Object object) {
			container.removeView(viewLists.get(position));
			
		}
		
		View.OnClickListener SelectItem = new View.OnClickListener() {
			@Override
			public void onClick(View v) {
		        switch(v.getId()){
		        
		        case R.id.self_bottom_layout:
		        	listView.setVisibility(View.GONE);
		        	viewshow = false;
		        	break;
		        	
		        case R.id.other_bottom_layout:
		        	listView.setVisibility(View.GONE);
		        	viewshow = false;
		        	break;
		        
		        case R.id.vts_iv_cry:
		        	resetView();
		        	iv_cry.setImageResource(R.drawable.type_icon1_pressed);
		        	typetext.setText(R.string.note_negative);
		        	
		        	//SetListItem(R.array.note_negative);
		        	SetListItem2(1, true);
		        	//listViewShowHide();
	        		//SetItem(sp_item,R.array.note_negative);
	        		//spinner_content.performClick();
	        		type = 1;
	        		break;
		        case R.id.vts_iv_not_good:
		        	resetView();
		        	iv_not_good.setImageResource(R.drawable.type_icon2_pressed);
		        	typetext.setText(R.string.note_notgood);
		        	
		        	//SetListItem(R.array.note_notgood);
		        	SetListItem2(2, true);
		        	//SetItem(sp_item,R.array.note_notgood);
		        	//spinner_content.performClick();
		        	type = 2;
			        break;
		        case R.id.vts_iv_smile:
		        	resetView();
		        	iv_smile.setImageResource(R.drawable.type_icon3_pressed);
		        	typetext.setText(R.string.note_positive);
		        	
		        	//SetListItem(R.array.note_positive);
		        	SetListItem2(3, true);
		        	//SetItem(sp_item, R.array.note_positive);
		        	//spinner_content.performClick();
		        	type = 3;
		        	break;
		        case R.id.vts_iv_try:
		        	resetView();
		        	iv_try.setImageResource(R.drawable.type_icon4_pressed);
		        	typetext.setText(R.string.note_selftest);
		        	
		        	//SetListItem(R.array.note_selftest);
		        	SetListItem2(4, true);
		        	//SetItem(sp_item,R.array.note_selftest);
		        	//sp_item.performClick();
		        	type = 4; 
		        	break;
		        case R.id.vts_iv_urge:
		        	resetView();
		        	iv_urge.setImageResource(R.drawable.type_icon5_pressed);
		        	typetext.setText(R.string.note_temptation);
		        	
		        	//SetListItem(R.array.note_temptation);
		        	SetListItem2(5, true);
		        	//SetItem(sp_item,R.array.note_temptation);
		        	//sp_item.performClick();
		        	type = 5;
		        	break;
		        case R.id.vts_iv_playing:
		        	resetView();
		        	iv_playing.setImageResource(R.drawable.type_icon8_pressed);
		        	typetext.setText(R.string.note_play);
		        	
		        	//SetListItem(R.array.note_play);
		        	SetListItem2(8, true);
		        	//SetItem(sp_item,R.array.note_play);
		        	//sp_item.performClick();
		        	type = 8;
		        	break;
		        case R.id.vts_iv_social:
		        	resetView();
		        	iv_social.setImageResource(R.drawable.type_icon7_pressed);
		        	typetext.setText(R.string.note_social);
		        	
		        	//SetListItem(R.array.note_social);
		        	SetListItem2(7, true);
		        	//SetItem(sp_item,R.array.note_social);
		        	//sp_item.performClick();
		        	type = 7;
		        	break;
		        case R.id.vts_iv_conflict:
		        	resetView();
		        	iv_conflict.setImageResource(R.drawable.type_icon6_pressed);
		        	typetext.setText(R.string.note_conflict);
		        	
		        	//SetListItem(R.array.note_conflict);
		        	SetListItem2(6, true);
		        	//SetItem(sp_item,R.array.note_conflict);
		        	//sp_item.performClick();
		        	type = 6;
		        	break;
		        	
		        }
			}
		};
	}
	public class MyClickListener implements OnClickListener
	{
		private int index = 0;
		public MyClickListener(int i){
			index = i;
		}
		
		@Override
		public void onClick(View arg0) {
			vPager.setCurrentItem(index);
			switch(index){
			case 0:
				ClickLog.Log(ClickLogId.DAYBOOK_ADDNOTE_CLICK_SELF);
				text_self.setTextColor(resource.getColor(R.color.blue));
				text_other.setTextColor(resource.getColor(R.color.text_gray3));
				iv_self_others_bar.setImageResource(R.drawable.note_slide_line1);
				break;
			case 1:
				ClickLog.Log(ClickLogId.DAYBOOK_ADDNOTE_CLICK_OTHER);
				text_self.setTextColor(resource.getColor(R.color.text_gray3));
				text_other.setTextColor(resource.getColor(R.color.blue));
				iv_self_others_bar.setImageResource(R.drawable.note_slide_line2);
				break;

			}
		}
		
	}
	
	
	//監聽頁面切換時間,主要做的是動畫處理,就是移動條的移動
		public class MyOnPageChangeListener implements OnPageChangeListener {


			@Override
			public void onPageSelected(int index) {

				switch (index) {
				case 0:
					ClickLog.Log(ClickLogId.DAYBOOK_ADDNOTE_SCROLL_SELF);
					text_self.setTextColor(resource.getColor(R.color.blue));
					text_other.setTextColor(resource.getColor(R.color.text_gray3));
					iv_self_others_bar.setImageResource(R.drawable.note_slide_line1);
					break;
				case 1:
					ClickLog.Log(ClickLogId.DAYBOOK_ADDNOTE_SCROLL_OTHER);
					text_self.setTextColor(resource.getColor(R.color.text_gray3));
					text_other.setTextColor(resource.getColor(R.color.blue));
					iv_self_others_bar.setImageResource(R.drawable.note_slide_line2);
					break;
				}

			}
			@Override
			public void onPageScrollStateChanged(int arg0) {}

			@Override
			public void onPageScrolled(int arg0, float arg1, int arg2) {}
		
		}
		
		public float getDensity(){
			 DisplayMetrics metrics = resource.getDisplayMetrics();
			 return metrics.density;
			}
		
		public float convertDpToPixel(float dp){
		    float px = dp * getDensity();
		    return px;
		}
		/**
		 * Sets ListView height dynamically based on the height of the items.   
		 *
		 *
		 * @return true if the listView is successfully resized, false otherwise
		 */
		public boolean setListViewHeightBasedOnItems(ListView listview) {

		    ListAdapter listAdapter = listview.getAdapter();
		    if (listAdapter != null) {
		    	
		        int numberOfItems = listAdapter.getCount();
		        
		        //listview2 : mood
		        /*if(listview == listView2)
		        {
		        	moodNum = numberOfItems;
		        }*/
		        
		        // Get total height of all items.
		        int totalItemsHeight = 0;
		        Log.d("GG", "here item number : "+ numberOfItems);
		        for (int itemPos = 0; itemPos < numberOfItems; itemPos++) {
		            View item = listAdapter.getView(itemPos, null, listview);
		            
		            item.measure(0, 0);
		            
		            totalItemsHeight += item.getMeasuredHeight();
		        }
		        
		        // Get total height of all item dividers.
		        int totalDividersHeight = listview.getDividerHeight() * 
		                (numberOfItems - 1);
		        
		        // Set list height.
		        ViewGroup.LayoutParams params = listview.getLayoutParams();
		        //params.height = totalItemsHeight + totalDividersHeight;
		        params.height = (int) (convertDpToPixel((float)40)* numberOfItems) + totalDividersHeight;
		        
		        listview.setLayoutParams(params);
		        listview.requestLayout();
		        	
		        //Log.d("GG", "Iheight : "+totalItemsHeight);
		        //Log.d("GG", "Dheight : "+totalDividersHeight);
		        
		        return true;

		    } else {
		        return false;
		    }

		}
		
		private void checkAndSetTimeSlot(){ 
			Calendar cal = Calendar.getInstance();
			int hours = cal.get(Calendar.HOUR_OF_DAY);
			int time_slot = TimeBlock.getTimeBlock(hours);
			timeslot = time_slot;
			timeslot_txt.setText(Timeslot_str[time_slot]);
		}

		@Override
		public void resetView(int type, int select) {
			setEnabledAll(boxLayout, true);
			Log.d("GG", "reset view");
			thinking_text.setEnabled(true);
			thinking_text.setInputType(InputType.TYPE_CLASS_TEXT);
			thinking_text.setFocusable(true);
			thinking_text.setFocusableInTouchMode(true);
			
			SetListItemFeeling();
			//SetListItem2(this.type, false);
			
			if(select == -1) //什麼都沒選
				return;
			
			if(type == 1){
				day = select;
				date_txt.setText(Date_str[select]);
				
				if(day == 0)//在別天選晚上時段, 回到今天還是要擋掉未來時段
					checkAndSetTimeSlot();
			}
			else{
				timeslot = select;
				timeslot_txt.setText(Timeslot_str[select]);
			}
			
			
		}
		
		/* feeling : listview item*/
		private void SetListItemFeeling(){
			//listView2.setVisibility(View.GONE);
			//viewshow2 = false;
			
			String[] type1 = PreferenceControl.getTypeMood();
			String[] after = clean(type1);
			
			final Integer[] imageId = {
					R.drawable.mood_angry,
		            R.drawable.mood_sad,
		            R.drawable.mood_nervous,
		            R.drawable.mood_hate,
		            R.drawable.mood_happy,
		            R.drawable.mood_afraid,
		            R.drawable.mood_boring
		    };
			
			final Integer[] imageIdClicked = {
					R.drawable.mood_angry_clicked,
		            R.drawable.mood_sad_clicked,
		            R.drawable.mood_nervous_clicked,
		            R.drawable.mood_hate_clicked,
		            R.drawable.mood_happy_clicked,
		            R.drawable.mood_afraid_clicked,
		            R.drawable.mood_boring_clicked
		    };
			
			final Integer[] all_imageId = {
					R.drawable.mood_angry,
		            R.drawable.mood_sad,
		            R.drawable.mood_nervous,
		            R.drawable.mood_hate,
		            R.drawable.mood_happy,
		            R.drawable.mood_afraid,
		            R.drawable.mood_calm,
		            R.drawable.mood_relax,
		            R.drawable.mood_excited,
		            R.drawable.mood_objective,
		            R.drawable.mood_happy,
		            R.drawable.mood_boring,
		            R.drawable.mood_energy,
		            R.drawable.mood_loved,
		            R.drawable.mood_objective
		    };
			
			final Integer[] all_imageIdClicked = {
					R.drawable.mood_angry_clicked,
		            R.drawable.mood_sad_clicked,
		            R.drawable.mood_nervous_clicked,
		            R.drawable.mood_hate_clicked,
		            R.drawable.mood_happy_clicked,
		            R.drawable.mood_afraid_clicked,
		            R.drawable.mood_calm_clicked,
		            R.drawable.mood_relax_clicked,
		            R.drawable.mood_excited_clicked,
		            R.drawable.mood_objective_clicked,
		            R.drawable.mood_happy_clicked,
		            R.drawable.mood_boring_clicked,
		            R.drawable.mood_energy_clicked,
		            R.drawable.mood_loved_clicked,
		            R.drawable.mood_objective_clicked
		    };
			
			//ArrayAdapter adapter = new ArrayAdapter<String>(context, R.layout.my_listitem, after);
			CustomList adapter = new CustomList(activity, after, imageId);
			
			listView2.setAdapter(adapter);
			listView2.setOnItemClickListener(new OnItemClickListener(){

			   @Override
			   public void onItemClick(AdapterView<?> parent, View view, int position, long id){
				   //ClickLog.Log(ClickLogId.DAYBOOK_ADDNOTE_SELECT_ITEM);	
				   
				    TextView c = (TextView) view.findViewById(R.id.text2);
				    ImageView m = (ImageView) view.findViewById(R.id.img);
				    
				    String playerChanged = c.getText().toString();
				    String showText = "";
				    
				    int nowMood = noteCategory.myNewHashMap.get(playerChanged);
				    
				    int tm = 0;
					for(int i = 0; i < moodNum; i++)
					{
					    if(!moodFlag[i])
					    	continue;
					   	tm++;
					}
					if(tm >= 3 && !moodFlag[nowMood - 900])
					   return;
				    
				   	if(moodFlag[nowMood - 900]){   //取消反白
				   		view.setBackgroundColor(0);
				   		m.setImageResource(all_imageId[nowMood - 900]);
				   	}
				   	else{ 						//反白
				   		view.setBackgroundColor(0xffdcdcdc);
				   		m.setImageResource(all_imageIdClicked[nowMood - 900]);
				   	}
					
					
				   	moodFlag[nowMood - 900] = !moodFlag[nowMood - 900];
				   	

				   	
				    //Toast.makeText(Settings.this,playerChanged, Toast.LENGTH_SHORT).show();  
				    for(int i = 0; i < moodNum; i++)
				    {
				    	if(!moodFlag[i])
				    		continue;
				    	String ts =  noteCategory.dictionary.get(i+900);
				    	if(showText != "")
				    		showText += ", ";
				    	showText += ts;
				    	
				    		
				    }
				   	
					edtext.setText(showText);
				    //listView2.setVisibility(View.GONE);
				    //viewshow2 = false;
			   }
			   
			});
			
			setListViewHeightBasedOnItems(listView2);
			listView2.setVisibility(View.GONE);
			viewshow2 = false;
			sv.smoothScrollTo(0 , (int)convertDpToPixel((float)200));
		}
		
		 public void hideKeyboard(View view) {
		        InputMethodManager inputMethodManager = (InputMethodManager)context.getSystemService(Activity.INPUT_METHOD_SERVICE);
		        inputMethodManager.hideSoftInputFromWindow(view.getWindowToken(), 0);
		    }
		 
		public void closeall()
		{
			close();
			clear();
		}
		
		public void setIsTesting(boolean is, int _type)
		{
			testing = is;
			type = _type;
		}
		
		public static void goResult(){
			//!!!CustomToast.generateToast(R.string.after_test_pass, 2);
			MainActivity.getMainActivity().changeTab(1);
		}
		
		public int sendNoteAdd(){
			impact = impactSeekBar.getProgress();
			int nowKey = testQuestionCaller.writeQuestionFile(day, timeslot, type, items, impact, thinking_text.getText().toString(),edtext.getText().toString(),null,0 ,0);
			
			
			if(testing)
			{
				copingSetting();
				
			}
			else
			{
				close();
				clear();
			}
			
			Calendar cal = Calendar.getInstance();
			long ts = cal.getTimeInMillis();
			
			long pre_ts = PreferenceControl.getLastestNoteAddTimestamp();
			Date d = new Date(pre_ts);
			Calendar pre_cal = Calendar.getInstance();
			pre_cal.setTime(d);
			
			boolean sameDay = cal.get(Calendar.YEAR) == pre_cal.get(Calendar.YEAR) &&
	                  cal.get(Calendar.DAY_OF_YEAR) == pre_cal.get(Calendar.DAY_OF_YEAR);
			if(!sameDay)
			{
				CustomToast.generateToast(R.string.add_note_no_thinking, 1);
				AddScore nowScore = new AddScore(System.currentTimeMillis(), 1, 0, "填寫事件", 0, AddScore.NOTE);
				db.insertAddScore(nowScore);
				
			}
			PreferenceControl.setLastestNoteAddTimestamp(ts);
			return nowKey;
		}

	public int getThisItems()
	{
		return items;
	}
		
		/*@Override
	    public boolean onTouch(View view, MotionEvent motionEvent) {
	        //触摸的是EditText并且当前EditText可以滚动则将事件交给EditText处理；否则将事件交由其父类处理
	        if (view instanceof EditText || view instanceof ListView) {
	            view.getParent().requestDisallowInterceptTouchEvent(true);
	            if (motionEvent.getAction() == MotionEvent.ACTION_UP) {
	                view.getParent().requestDisallowInterceptTouchEvent(false);
	            }
	        }
	        return false;
	    }*/
}
