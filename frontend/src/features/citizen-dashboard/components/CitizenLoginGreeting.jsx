import React from 'react';
import PostLoginGreeting, { POST_LOGIN_GREETING_DURATION_MS } from '../../../auth/components/PostLoginGreeting';

export const CITIZEN_LOGIN_GREETING_DURATION_MS = POST_LOGIN_GREETING_DURATION_MS;

export default function CitizenLoginGreeting(props) {
  return <PostLoginGreeting {...props} audience="citizen" />;
}
