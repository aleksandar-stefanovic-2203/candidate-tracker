export interface ApplicationSubmission {
  name: string;
  email: string;
  phone_number: string;
  job_position_id: number | null;
  recommendation: string;
  message: string | null;
  consent_given_at: string;
}

export interface ApplicationSubmissionResponse {
  id: number;
  status: string;
  received_at: string;
}
