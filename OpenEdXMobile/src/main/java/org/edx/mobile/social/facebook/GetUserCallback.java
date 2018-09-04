package org.edx.mobile.social.facebook;

import android.support.annotation.NonNull;
import android.support.annotation.Nullable;

import com.facebook.GraphRequest;
import com.facebook.GraphResponse;

import org.edx.mobile.social.SocialMember;
import org.json.JSONException;
import org.json.JSONObject;

public class GetUserCallback {
    public interface IGetUserResponse {
        void onCompleted(@Nullable SocialMember socialMember);
    }

    private IGetUserResponse getUserResponse;
    private GraphRequest.Callback callback;

    public GetUserCallback(final IGetUserResponse getUserResponse) {
        this.getUserResponse = getUserResponse;
        callback = new GraphRequest.Callback() {
            @Override
            public void onCompleted(GraphResponse response) {
                SocialMember socialMember = null;
                try {
                    JSONObject userObj = response.getJSONObject();
                    if (userObj == null) {
                        return;
                    }
                    socialMember = jsonToUser(userObj);

                } catch (JSONException e) {
                    // Handle exception ...
                }

                // Handled by ProfileActivity
                GetUserCallback.this.getUserResponse.onCompleted(socialMember);
            }
        };
    }

    private SocialMember jsonToUser(@NonNull JSONObject user) throws JSONException {
        final String name = user.getString("name");
        final String id = user.getString("id");
        String email = null;
        if (user.has("email")) {
            email = user.getString("email");
        }
        final SocialMember member = new SocialMember(Long.parseLong(id), name);
        member.setEmail(email);
        return member;
    }

    public GraphRequest.Callback getCallback() {
        return callback;
    }
}
