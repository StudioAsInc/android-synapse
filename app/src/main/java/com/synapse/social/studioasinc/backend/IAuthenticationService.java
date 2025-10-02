package com.synapse.social.studioasinc.backend;

import com.synapse.social.studioasinc.model.User;

public interface IAuthenticationService {
    User getCurrentUser();
}