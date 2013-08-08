################################################################################
### Copyright NetApp Inc.  All rights reserved                               ###
### Author: ktenzer                                                          ###
###                                                                          ###
### Date: 09/11/2011                                                         ###
### Name: SAMPLE		                                             ###
################################################################################

################################################################################
### Changes ####################################################################
################################################################################
### Ver Author      Description                                              ###
### 1.0 ktenzer      Initial Version                                         ###
################################################################################
package ZIMBRA;
$| = 1;
our @ISA = qw(SnapCreator::Mod);

=head1 NAME
=cut

=head1 DESCRIPTION
=cut

use strict;
use warnings;
use diagnostics;
use SnapCreator::Util::Generic qw ( trim isEmpty );
use SnapCreator::Util::OS qw ( isWindows isUnix getUid createTmpFile );
use SnapCreator::Event qw ( INFO ERROR WARN DEBUG COMMENT ASUP CMD DUMP );
use File::Basename;
use File::Temp qw/ tempfile /;
use FileHandle;
use IPC::Open3;
#use Mail::Mailer;

### Objects ###
#my $configObj = new SnapCreator::Util::Config();
my $msgObj = new SnapCreator::Event();
my %config_h = ();
my $osObj = new SnapCreator::Util::OS();
my $result = {
  exit_code => 0,
  stdout => 'Finished successfully',
  stderr => ''
};
=head2 new()
	Creates a new SAMPLE instance. 
=cut

sub new {
   my $invocant = shift;
   my $class = ref($invocant) || $invocant;
        my $self = {
                @_
        };
        bless ($self, $class);
        return $self;
}


=head2 setENV(%environment)
	
	Sets the environment variables. 
	
=cut 
sub setENV {
  my($self, $obj) = @_;
  %config_h = %{$obj};
  my @message_a = ();
  #$msgObj->collect(\@message_a, INFO, "$config_h{'APP_NAME'}::setENV");
  $result->{message} = \@message_a;
  return $result;
}

sub suspendAllAccounts {
   my $result = $osObj->execute("$config_h{'ZIMBRA_PLUGIN_PATH'}run.sh maintenance $config_h{'ZIMBRA_HOST'} $config_h{'SNAPCREATOR_THREAD_COUNT'}"); 
   return $result;
} 
sub restoreAllAccounts() {
  my $result = $osObj->execute("$config_h{'ZIMBRA_PLUGIN_PATH'}run.sh active $config_h{'ZIMBRA_HOST'} $config_h{'SNAPCREATOR_THREAD_COUNT'}");
  return $result;
}

sub quiesce {
	my @message_a = ();
        my $result = {
                exit_code => 0,
                stdout => "",
                stderr => "",
        };
        my $cmd = "";
        $msgObj->collect(\@message_a, INFO, "1.Quiescing plug-in $config_h{'APP_NAME'}");
        $msgObj->collect(\@message_a, INFO, " with parameters: SNAPCREATOR_THREAD_COUNT=$config_h{'SNAPCREATOR_THREAD_COUNT'};ZIMBRA_HOST=$config_h{'ZIMBRA_HOST'}");	
	$result = suspendAllAccounts();
        $msgObj->collect(\@message_a, INFO, "1.Quiescing plug-in $config_h{'APP_NAME'} finished successfully");

        if ($result->{exit_code} != 0) {
                $msgObj->collect(\@message_a, ERROR, "Command [$cmd] failed with return code $result->{exit_code}");
                push (@message_a, @{$result->{message}}) if exists ($result->{message});
                goto END;
        }
	END:
        $result->{message} = \@message_a;
	return $result;
}

sub unquiesce() {
       my $result = {
                exit_code => 0,
                stdout => "",
                stderr => "",
        };
	my @message_a = ();
        $msgObj->collect(\@message_a, INFO, "Unquiescing plug-in $config_h{'APP_NAME'}");
	restoreAllAccounts();
        $msgObj->collect(\@message_a, INFO, "Unquiescing plug-in $config_h{'APP_NAME'} finished successfully");

	END:
        $result->{message} = \@message_a;

        return $result;
}

sub restore {

        my $result = {
                exit_code => 0,
                stdout => "",
                stderr => "",
        };

	my @message_a = ();

        $msgObj->collect(\@message_a, INFO, "restore plug-in $config_h{'APP_NAME'} completed");

        $result->{message} = \@message_a;

        return $result;
}

sub restore_pre {
        my $result = {
                exit_code => 0,
                stdout => "",
                stderr => "",
        };

	my @message_a = ();

        $msgObj->collect(\@message_a, INFO, "restore_pre plug-in $config_h{'APP_NAME'} completed");

        $result->{message} = \@message_a;

        return $result;
}

sub restore_post {
        my $result = {
                exit_code => 0,
                stdout => "",
                stderr => "",
        };

	my @message_a = ();

        $msgObj->collect(\@message_a, INFO, "restore_post snapshot name is $config_h{'RESTORE_SNAP_NAME'}");
        $msgObj->collect(\@message_a, INFO, "restore_post plug-in $config_h{'APP_NAME'} completed");

        $result->{message} = \@message_a;

        return $result;
}

sub restore_cleanup {
        my $result = {
                exit_code => 0,
                stdout => "",
                stderr => "",
        };

	my @message_a = ();

        $msgObj->collect(\@message_a, INFO, "restore_cleanup plug-in $config_h{'APP_NAME'} completed");

        $result->{message} = \@message_a;

        return $result;
}
1;
