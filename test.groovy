def JOB_NAME = "beta-AI-751"
def branch = (JOB_NAME.toUpperCase() =~  /(AI-\d+)/)
def jiraKey = branch[0][1]
println jiraKey
return [ JIRA_KEY: jiraKey ];