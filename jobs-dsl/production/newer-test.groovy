job('test-folder/sample-job-1') {
    description('Sample job inside test-folder')
    steps {
        shell('echo "Hello from sample-job-1"')
    }
}

job('test-folder/sample-job-2') {
    description('Another sample job')
    steps {
        shell('echo "Hello from sample-job-2"')
    }
}
