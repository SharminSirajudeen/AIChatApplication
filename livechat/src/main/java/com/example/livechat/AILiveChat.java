package com.example.livechat;

import android.annotation.SuppressLint;
import android.content.Context;
import android.graphics.Bitmap;
import android.os.AsyncTask;
import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.text.format.Time;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import com.google.gson.JsonElement;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.Locale;
import java.util.Map;

import ai.api.AIDataService;
import ai.api.AIListener;
import ai.api.AIServiceException;
import ai.api.android.AIConfiguration;
import ai.api.model.AIError;
import ai.api.model.AIRequest;
import ai.api.model.AIResponse;
import ai.api.model.Metadata;
import ai.api.model.Result;
import ai.api.model.Status;

public class AILiveChat extends AppCompatActivity implements View.OnClickListener, AIListener {
    private RecyclerView recyclerChat;
    public static final int VIEW_TYPE_USER_MESSAGE = 0;
    public static final int VIEW_TYPE_FRIEND_MESSAGE = 1;
    private ListMessageAdapter adapter;
    private String roomId;
    private ArrayList<CharSequence> idFriend;
    private Consersation consersation;
    private ImageButton btnSend;
    private EditText editWriteMessage;
    private LinearLayoutManager linearLayoutManager;
    public static HashMap<String, Bitmap> bitmapAvataFriend;
    public Bitmap bitmapAvataUser;
    AIRequest aiRequest;
    AIDataService aiDataService;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.layout_to_include);
        final AIConfiguration config = new AIConfiguration("d9fcd9bfb2f2468394016b11642a1d5c",
                AIConfiguration.SupportedLanguages.English,
                AIConfiguration.RecognitionEngine.System);
//        AIService aiService = AIService.getService(AILiveChat.this, config);

        aiDataService = new AIDataService(config);

        aiRequest = new AIRequest();
        consersation = new Consersation();
        btnSend = (ImageButton) findViewById(R.id.btnSend);
        btnSend.setOnClickListener(this);
        editWriteMessage = (EditText) findViewById(R.id.editWriteMessage);
        getSupportActionBar().setTitle("Talabat Assistant");
        linearLayoutManager = new LinearLayoutManager(this, LinearLayoutManager.VERTICAL, false);
        recyclerChat = (RecyclerView) findViewById(R.id.recyclerChat);
        recyclerChat.setLayoutManager(linearLayoutManager);
        adapter = new ListMessageAdapter(this, consersation, bitmapAvataFriend, bitmapAvataUser);
        recyclerChat.setAdapter(adapter);
    }

    @Override
    public void onClick(View v) {
        if (v.getId() == R.id.btnSend) {
            String content = editWriteMessage.getText().toString().trim();
            if (!isNullOrEmpty(content)) {
                aiRequest.setQuery(content);
                sendTheRequest();
                if (content.length() > 0) {
                    editWriteMessage.setText("");
                    Message newMessage = new Message();
                    newMessage.text = content;
                    newMessage.idSender = "UserIdUnique";
                    newMessage.timestamp = System.currentTimeMillis();
                    newMessage.displayTime = getTime();
                    addToView(newMessage);
                }
            }
        }
    }

    @SuppressLint("StaticFieldLeak")
    private void sendTheRequest() {
        new AsyncTask<AIRequest, Void, AIResponse>() {
            @Override
            protected AIResponse doInBackground(AIRequest... requests) {
                final AIRequest request = requests[0];
                try {
                    final AIResponse aiResponse = aiDataService.request(aiRequest);
                    return aiResponse;
                } catch (AIServiceException e) {
                }
                return null;
            }

            @Override
            protected void onPostExecute(AIResponse aiResponse) {
                if (aiResponse != null) {
                    Result result = aiResponse.getResult();
                    ai.api.model.Status status = aiResponse.getStatus();
                    String speech = result.getFulfillment().getSpeech();
                    Metadata metadata = result.getMetadata();
                    if (metadata != null) {
                        Log.i("TAGAPIAI", "Intent id: " + metadata.getIntentId());
                        Log.i("TAGAPIAI", "Intent name: " + metadata.getIntentName());
                    }

                    // Get parameters
                    String parameterString = "";
                    if (result.getParameters() != null && !result.getParameters().isEmpty()) {
                        for (final Map.Entry<String, JsonElement> entry : result.getParameters().entrySet()) {
                            parameterString += "(" + entry.getKey() + ", " + entry.getValue() + ") ";
                        }
                    }

                    Message newMessage = new Message();
                    newMessage.idSender = "API.AI";
                    newMessage.idReceiver = "UserIdUnique";
                    newMessage.text = speech;
                /*"\n\nIntent id: " + metadata.getIntentId()+"\n\nIntent name: " + metadata.getIntentName()+*/
                    ;
                    newMessage.timestamp = System.currentTimeMillis();
                    newMessage.displayTime = getTime();
                    addToView(newMessage);
                }
            }
        }.execute(aiRequest);
    }

    @Override
    public void onResult(AIResponse aiResponse) {
        Result result = aiResponse.getResult();
        Status status = aiResponse.getStatus();
        String speech = result.getFulfillment().getSpeech();
        Metadata metadata = result.getMetadata();

        // Get parameters
        String parameterString = "";
        if (result.getParameters() != null && !result.getParameters().isEmpty()) {
            for (final Map.Entry<String, JsonElement> entry : result.getParameters().entrySet()) {
                parameterString += "(" + entry.getKey() + ", " + entry.getValue() + ") ";
            }
        }

        Message newMessage = new Message();
        newMessage.idSender = "API.AI";
        newMessage.idReceiver = "UserIdUnique";
        newMessage.text = "Query:" + result.getResolvedQuery() +
                "\nAction: " + result.getAction() +
                "\nParameters: " + parameterString +
                "\n\nStatus code: " + status.getCode() +
                "\n\nStatus type: " + status.getErrorType() +
                "\n\nSpeech: " + speech
                /*"\n\nIntent id: " + metadata.getIntentId()+"\n\nIntent name: " + metadata.getIntentName()+*/;
        newMessage.timestamp = System.currentTimeMillis();
        newMessage.displayTime = getTime();
        addToView(newMessage);

    }

    public String getTime() {
        Time time = new Time(Time.getCurrentTimezone());
        time.setToNow();
        String formattedDate = time.format("%d/%m/%Y");
        Date date = new Date(System.currentTimeMillis());
        SimpleDateFormat dateFormat = new SimpleDateFormat("hh:mm:ss aa", Locale.ENGLISH);
        String var = dateFormat.format(date);
        return var + ", " + formattedDate;
    }

    private void addToView(Message newMessage) {
        consersation.getListMessageData().add(newMessage);
        adapter.notifyDataSetChanged();
        linearLayoutManager.scrollToPosition(consersation.getListMessageData().size() - 1);
    }

    @Override
    public void onError(AIError error) {
        Toast.makeText(this, error + "", Toast.LENGTH_SHORT).show();
    }

    @Override
    public void onAudioLevel(float level) {

    }

    @Override
    public void onListeningStarted() {

    }

    @Override
    public void onListeningCanceled() {

    }

    @Override
    public void onListeningFinished() {

    }

    public boolean isNullOrEmpty(String str) {
        if (str == null) {
            return true;
        } else return str.trim().equals("");
    }


    class ListMessageAdapter extends RecyclerView.Adapter<RecyclerView.ViewHolder> {

        private Context context;
        private Consersation consersation;
        private HashMap<String, Bitmap> bitmapAvata;
        //    private HashMap<String, DatabaseReference> bitmapAvataDB;
        private Bitmap bitmapAvataUser;

        public ListMessageAdapter(Context context, Consersation consersation, HashMap<String, Bitmap> bitmapAvata, Bitmap bitmapAvataUser) {
            this.context = context;
            this.consersation = consersation;
            this.bitmapAvata = bitmapAvata;
            this.bitmapAvataUser = bitmapAvataUser;
//        bitmapAvataDB = new HashMap<>();
        }

        @Override
        public RecyclerView.ViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
            if (viewType == AILiveChat.VIEW_TYPE_FRIEND_MESSAGE) {
                View view = LayoutInflater.from(context).inflate(R.layout.rc_item_message_friend, parent, false);
                return new ItemMessageFriendHolder(view);
            } else if (viewType == AILiveChat.VIEW_TYPE_USER_MESSAGE) {
                View view = LayoutInflater.from(context).inflate(R.layout.rc_item_message_user, parent, false);
                return new ItemMessageUserHolder(view);
            }
            return null;
        }

        @Override
        public void onBindViewHolder(RecyclerView.ViewHolder holder, int position) {
            if (holder instanceof ItemMessageFriendHolder) {
                ((ItemMessageFriendHolder) holder).txtContent.setText(consersation.getListMessageData().get(position).text);
                ((ItemMessageFriendHolder) holder).time.setText(consersation.getListMessageData().get(position).displayTime);
            } else if (holder instanceof ItemMessageUserHolder) {
                ((ItemMessageUserHolder) holder).txtContent.setText(consersation.getListMessageData().get(position).text);
                ((ItemMessageUserHolder) holder).time.setText(consersation.getListMessageData().get(position).displayTime);
            }
        }

        @Override
        public int getItemViewType(int position) {
            return consersation.getListMessageData().get(position).idSender.equals("UserIdUnique") ? AILiveChat.VIEW_TYPE_USER_MESSAGE : AILiveChat.VIEW_TYPE_FRIEND_MESSAGE;
        }

        @Override
        public int getItemCount() {
            return consersation.getListMessageData().size();
        }
    }

    class ItemMessageUserHolder extends RecyclerView.ViewHolder {
        public TextView txtContent;
        public ImageView avata;
        public TextView time;

        public ItemMessageUserHolder(View itemView) {
            super(itemView);
            txtContent = (TextView) itemView.findViewById(R.id.textContentUser);
            avata = (ImageView) itemView.findViewById(R.id.imageView2);
            time = itemView.findViewById(R.id.time);
        }
    }

    class ItemMessageFriendHolder extends RecyclerView.ViewHolder {
        public TextView txtContent;
        public ImageView avata;
        public TextView time;

        public ItemMessageFriendHolder(View itemView) {
            super(itemView);
            txtContent = (TextView) itemView.findViewById(R.id.textContentFriend);
            avata = (ImageView) itemView.findViewById(R.id.imageView3);
            time = itemView.findViewById(R.id.time);
        }
    }

}
